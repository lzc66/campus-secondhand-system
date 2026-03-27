package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.entity.WantedPost;
import com.campus.secondhand.enums.WantedPostStatus;
import com.campus.secondhand.mapper.ItemCategoryMapper;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.mapper.WantedPostMapper;
import com.campus.secondhand.service.PublicWantedPostService;
import com.campus.secondhand.vo.publicapi.PublicWantedPostDetailResponse;
import com.campus.secondhand.vo.publicapi.PublicWantedPostPageResponse;
import com.campus.secondhand.vo.publicapi.PublicWantedPostSummaryResponse;
import com.campus.secondhand.vo.publicapi.PublicWantedRequesterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class PublicWantedPostServiceImpl implements PublicWantedPostService {

    private final WantedPostMapper wantedPostMapper;
    private final ItemCategoryMapper itemCategoryMapper;
    private final UserMapper userMapper;
    private final MediaFileMapper mediaFileMapper;

    public PublicWantedPostServiceImpl(WantedPostMapper wantedPostMapper,
                                       ItemCategoryMapper itemCategoryMapper,
                                       UserMapper userMapper,
                                       MediaFileMapper mediaFileMapper) {
        this.wantedPostMapper = wantedPostMapper;
        this.itemCategoryMapper = itemCategoryMapper;
        this.userMapper = userMapper;
        this.mediaFileMapper = mediaFileMapper;
    }

    @Override
    public PublicWantedPostPageResponse listWantedPosts(Long categoryId,
                                                        String keyword,
                                                        String brand,
                                                        BigDecimal priceMin,
                                                        BigDecimal priceMax,
                                                        String sortBy,
                                                        long page,
                                                        long size) {
        validatePriceRange(priceMin, priceMax);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedBrand = StringUtils.hasText(brand) ? brand.trim() : null;
        Page<WantedPost> queryPage = new Page<>(Math.max(page, 1), Math.max(size, 1));
        LambdaQueryWrapper<WantedPost> wrapper = new LambdaQueryWrapper<WantedPost>()
                .eq(WantedPost::getStatus, WantedPostStatus.OPEN)
                .isNull(WantedPost::getDeletedAt)
                .eq(categoryId != null, WantedPost::getCategoryId, categoryId)
                .eq(StringUtils.hasText(normalizedBrand), WantedPost::getBrand, normalizedBrand)
                .and(priceMin != null, q -> q.isNull(WantedPost::getExpectedPriceMax).or().ge(WantedPost::getExpectedPriceMax, priceMin))
                .and(priceMax != null, q -> q.isNull(WantedPost::getExpectedPriceMin).or().le(WantedPost::getExpectedPriceMin, priceMax))
                .and(StringUtils.hasText(normalizedKeyword), q -> q
                        .like(WantedPost::getTitle, normalizedKeyword)
                        .or().like(WantedPost::getBrand, normalizedKeyword)
                        .or().like(WantedPost::getModel, normalizedKeyword)
                        .or().like(WantedPost::getDescription, normalizedKeyword))
                .and(q -> q.isNull(WantedPost::getExpiresAt).or().gt(WantedPost::getExpiresAt, LocalDateTime.now()));
        applySort(wrapper, sortBy);
        Page<WantedPost> result = wantedPostMapper.selectPage(queryPage, wrapper);
        List<PublicWantedPostSummaryResponse> records = result.getRecords().stream()
                .map(this::buildSummaryResponse)
                .toList();
        return new PublicWantedPostPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    @Transactional
    public PublicWantedPostDetailResponse getWantedPostDetail(Long wantedPostId) {
        WantedPost wantedPost = getRequiredPublicWantedPost(wantedPostId);
        wantedPost.setViewCount(wantedPost.getViewCount() == null ? 1 : wantedPost.getViewCount() + 1);
        wantedPostMapper.updateById(wantedPost);
        return buildDetailResponse(wantedPost);
    }

    private PublicWantedPostSummaryResponse buildSummaryResponse(WantedPost wantedPost) {
        ItemCategory category = wantedPost.getCategoryId() == null ? null : itemCategoryMapper.selectById(wantedPost.getCategoryId());
        return new PublicWantedPostSummaryResponse(
                wantedPost.getWantedPostId(),
                wantedPost.getCategoryId(),
                category == null ? null : category.getCategoryName(),
                wantedPost.getTitle(),
                wantedPost.getBrand(),
                wantedPost.getModel(),
                wantedPost.getExpectedPriceMin(),
                wantedPost.getExpectedPriceMax(),
                wantedPost.getViewCount(),
                wantedPost.getExpiresAt(),
                wantedPost.getCreatedAt()
        );
    }

    private PublicWantedPostDetailResponse buildDetailResponse(WantedPost wantedPost) {
        ItemCategory category = wantedPost.getCategoryId() == null ? null : itemCategoryMapper.selectById(wantedPost.getCategoryId());
        User requester = userMapper.selectById(wantedPost.getRequesterUserId());
        MediaFile avatar = requester == null || requester.getAvatarFileId() == null ? null : mediaFileMapper.selectById(requester.getAvatarFileId());
        PublicWantedRequesterResponse requesterResponse = requester == null ? null : new PublicWantedRequesterResponse(
                requester.getUserId(),
                requester.getRealName(),
                avatar == null ? null : avatar.getFileUrl(),
                requester.getCollegeName(),
                requester.getMajorName(),
                requester.getClassName()
        );
        return new PublicWantedPostDetailResponse(
                wantedPost.getWantedPostId(),
                wantedPost.getCategoryId(),
                category == null ? null : category.getCategoryName(),
                wantedPost.getTitle(),
                wantedPost.getBrand(),
                wantedPost.getModel(),
                wantedPost.getDescription(),
                wantedPost.getExpectedPriceMin(),
                wantedPost.getExpectedPriceMax(),
                wantedPost.getContactPhone(),
                wantedPost.getContactQq(),
                wantedPost.getContactWechat(),
                wantedPost.getStatus().getValue(),
                wantedPost.getViewCount(),
                wantedPost.getExpiresAt(),
                wantedPost.getCreatedAt(),
                wantedPost.getUpdatedAt(),
                requesterResponse
        );
    }

    private WantedPost getRequiredPublicWantedPost(Long wantedPostId) {
        WantedPost wantedPost = wantedPostMapper.selectById(wantedPostId);
        if (wantedPost == null
                || wantedPost.getDeletedAt() != null
                || wantedPost.getStatus() != WantedPostStatus.OPEN
                || (wantedPost.getExpiresAt() != null && !wantedPost.getExpiresAt().isAfter(LocalDateTime.now()))) {
            throw new BusinessException(40470, HttpStatus.NOT_FOUND, "Wanted post not found");
        }
        return wantedPost;
    }

    private void applySort(LambdaQueryWrapper<WantedPost> wrapper, String sortBy) {
        String normalized = StringUtils.hasText(sortBy) ? sortBy.trim().toLowerCase(Locale.ROOT) : "latest";
        switch (normalized) {
            case "popular" -> wrapper.orderByDesc(WantedPost::getViewCount).orderByDesc(WantedPost::getCreatedAt);
            case "expiring" -> wrapper.orderByAsc(WantedPost::getExpiresAt).orderByDesc(WantedPost::getCreatedAt);
            default -> wrapper.orderByDesc(WantedPost::getCreatedAt);
        }
    }

    private void validatePriceRange(BigDecimal priceMin, BigDecimal priceMax) {
        if (priceMin != null && priceMin.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(40070, HttpStatus.BAD_REQUEST, "priceMin must be greater than or equal to 0");
        }
        if (priceMax != null && priceMax.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(40071, HttpStatus.BAD_REQUEST, "priceMax must be greater than or equal to 0");
        }
        if (priceMin != null && priceMax != null && priceMax.compareTo(priceMin) < 0) {
            throw new BusinessException(40072, HttpStatus.BAD_REQUEST, "priceMax must be greater than or equal to priceMin");
        }
    }
}
