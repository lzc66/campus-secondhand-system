package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.user.SaveWantedPostRequest;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.entity.WantedPost;
import com.campus.secondhand.enums.WantedPostStatus;
import com.campus.secondhand.mapper.ItemCategoryMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.mapper.WantedPostMapper;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.RecommendationBehaviorService;
import com.campus.secondhand.service.UserWantedPostService;
import com.campus.secondhand.vo.user.UserWantedPostDetailResponse;
import com.campus.secondhand.vo.user.UserWantedPostPageResponse;
import com.campus.secondhand.vo.user.UserWantedPostSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class UserWantedPostServiceImpl implements UserWantedPostService {

    private final UserMapper userMapper;
    private final WantedPostMapper wantedPostMapper;
    private final ItemCategoryMapper itemCategoryMapper;
    private final RecommendationBehaviorService recommendationBehaviorService;

    public UserWantedPostServiceImpl(UserMapper userMapper,
                                     WantedPostMapper wantedPostMapper,
                                     ItemCategoryMapper itemCategoryMapper,
                                     RecommendationBehaviorService recommendationBehaviorService) {
        this.userMapper = userMapper;
        this.wantedPostMapper = wantedPostMapper;
        this.itemCategoryMapper = itemCategoryMapper;
        this.recommendationBehaviorService = recommendationBehaviorService;
    }

    @Override
    @Transactional
    public UserWantedPostDetailResponse createWantedPost(UserPrincipal principal, SaveWantedPostRequest request) {
        User requester = getRequiredUser(principal.getUserId());
        validatePriceRange(request.expectedPriceMin(), request.expectedPriceMax());
        validateExpiresAt(request.expiresAt());
        ItemCategory category = getOptionalCategory(request.categoryId());
        WantedPost wantedPost = WantedPost.builder()
                .requesterUserId(principal.getUserId())
                .categoryId(category == null ? null : category.getCategoryId())
                .title(request.title().trim())
                .brand(trimToNull(request.brand()))
                .model(trimToNull(request.model()))
                .description(request.description().trim())
                .expectedPriceMin(request.expectedPriceMin())
                .expectedPriceMax(request.expectedPriceMax())
                .contactPhone(resolveContact(request.contactPhone(), requester.getPhone()))
                .contactQq(resolveContact(request.contactQq(), requester.getQqNo()))
                .contactWechat(resolveContact(request.contactWechat(), requester.getWechatNo()))
                .status(parseEditableStatus(request.status()))
                .expiresAt(request.expiresAt())
                .viewCount(0)
                .build();
        wantedPostMapper.insert(wantedPost);
        recommendationBehaviorService.recordWantedPostPublish(principal.getUserId(), wantedPost.getWantedPostId(), wantedPost.getTitle());
        return buildDetailResponse(wantedPost);
    }

    @Override
    @Transactional
    public UserWantedPostDetailResponse updateWantedPost(UserPrincipal principal, Long wantedPostId, SaveWantedPostRequest request) {
        WantedPost wantedPost = getOwnedWantedPost(principal.getUserId(), wantedPostId);
        User requester = getRequiredUser(principal.getUserId());
        validatePriceRange(request.expectedPriceMin(), request.expectedPriceMax());
        validateExpiresAt(request.expiresAt());
        ItemCategory category = getOptionalCategory(request.categoryId());

        wantedPost.setCategoryId(category == null ? null : category.getCategoryId());
        wantedPost.setTitle(request.title().trim());
        wantedPost.setBrand(trimToNull(request.brand()));
        wantedPost.setModel(trimToNull(request.model()));
        wantedPost.setDescription(request.description().trim());
        wantedPost.setExpectedPriceMin(request.expectedPriceMin());
        wantedPost.setExpectedPriceMax(request.expectedPriceMax());
        wantedPost.setContactPhone(resolveContact(request.contactPhone(), requester.getPhone()));
        wantedPost.setContactQq(resolveContact(request.contactQq(), requester.getQqNo()));
        wantedPost.setContactWechat(resolveContact(request.contactWechat(), requester.getWechatNo()));
        wantedPost.setStatus(parseEditableStatus(request.status()));
        wantedPost.setExpiresAt(request.expiresAt());
        wantedPostMapper.updateById(wantedPost);
        return buildDetailResponse(wantedPost);
    }

    @Override
    public UserWantedPostPageResponse listMyWantedPosts(UserPrincipal principal, String status, long page, long size) {
        WantedPostStatus statusFilter = parseListStatus(status);
        Page<WantedPost> queryPage = new Page<>(Math.max(page, 1), Math.max(size, 1));
        LambdaQueryWrapper<WantedPost> wrapper = new LambdaQueryWrapper<WantedPost>()
                .eq(WantedPost::getRequesterUserId, principal.getUserId())
                .isNull(WantedPost::getDeletedAt)
                .eq(statusFilter != null, WantedPost::getStatus, statusFilter)
                .orderByDesc(WantedPost::getCreatedAt);
        Page<WantedPost> result = wantedPostMapper.selectPage(queryPage, wrapper);
        List<UserWantedPostSummaryResponse> records = result.getRecords().stream()
                .map(this::buildSummaryResponse)
                .toList();
        return new UserWantedPostPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public UserWantedPostDetailResponse getMyWantedPost(UserPrincipal principal, Long wantedPostId) {
        return buildDetailResponse(getOwnedWantedPost(principal.getUserId(), wantedPostId));
    }

    @Override
    @Transactional
    public void deleteWantedPost(UserPrincipal principal, Long wantedPostId) {
        WantedPost wantedPost = getOwnedWantedPost(principal.getUserId(), wantedPostId);
        wantedPost.setStatus(WantedPostStatus.CANCELLED);
        wantedPost.setDeletedAt(LocalDateTime.now());
        wantedPostMapper.updateById(wantedPost);
    }

    private UserWantedPostSummaryResponse buildSummaryResponse(WantedPost wantedPost) {
        ItemCategory category = wantedPost.getCategoryId() == null ? null : itemCategoryMapper.selectById(wantedPost.getCategoryId());
        return new UserWantedPostSummaryResponse(
                wantedPost.getWantedPostId(),
                wantedPost.getCategoryId(),
                category == null ? null : category.getCategoryName(),
                wantedPost.getTitle(),
                wantedPost.getExpectedPriceMin(),
                wantedPost.getExpectedPriceMax(),
                wantedPost.getStatus().getValue(),
                wantedPost.getViewCount(),
                wantedPost.getExpiresAt(),
                wantedPost.getCreatedAt(),
                wantedPost.getUpdatedAt()
        );
    }

    private UserWantedPostDetailResponse buildDetailResponse(WantedPost wantedPost) {
        ItemCategory category = wantedPost.getCategoryId() == null ? null : itemCategoryMapper.selectById(wantedPost.getCategoryId());
        return new UserWantedPostDetailResponse(
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
                wantedPost.getUpdatedAt()
        );
    }

    private User getRequiredUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40420, HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }

    private ItemCategory getOptionalCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        ItemCategory category = itemCategoryMapper.selectById(categoryId);
        if (category == null || !Objects.equals(category.getIsEnabled(), 1)) {
            throw new BusinessException(40073, HttpStatus.BAD_REQUEST, "categoryId is invalid");
        }
        return category;
    }

    private WantedPost getOwnedWantedPost(Long userId, Long wantedPostId) {
        WantedPost wantedPost = wantedPostMapper.selectById(wantedPostId);
        if (wantedPost == null || wantedPost.getDeletedAt() != null || !Objects.equals(wantedPost.getRequesterUserId(), userId)) {
            throw new BusinessException(40470, HttpStatus.NOT_FOUND, "Wanted post not found");
        }
        return wantedPost;
    }

    private WantedPostStatus parseEditableStatus(String value) {
        String normalized = StringUtils.hasText(value) ? normalize(value) : "open";
        return switch (normalized) {
            case "open" -> WantedPostStatus.OPEN;
            case "matched" -> WantedPostStatus.MATCHED;
            case "closed" -> WantedPostStatus.CLOSED;
            case "cancelled" -> WantedPostStatus.CANCELLED;
            default -> throw new BusinessException(40074, HttpStatus.BAD_REQUEST, "status must be open, matched, closed, or cancelled");
        };
    }

    private WantedPostStatus parseListStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = normalize(value);
        return switch (normalized) {
            case "open" -> WantedPostStatus.OPEN;
            case "matched" -> WantedPostStatus.MATCHED;
            case "closed" -> WantedPostStatus.CLOSED;
            case "cancelled" -> WantedPostStatus.CANCELLED;
            default -> throw new BusinessException(40075, HttpStatus.BAD_REQUEST, "status filter is invalid");
        };
    }

    private void validatePriceRange(BigDecimal min, BigDecimal max) {
        if (min != null && min.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(40076, HttpStatus.BAD_REQUEST, "expectedPriceMin must be greater than or equal to 0");
        }
        if (max != null && max.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(40077, HttpStatus.BAD_REQUEST, "expectedPriceMax must be greater than or equal to 0");
        }
        if (min != null && max != null && max.compareTo(min) < 0) {
            throw new BusinessException(40078, HttpStatus.BAD_REQUEST, "expectedPriceMax must be greater than or equal to expectedPriceMin");
        }
    }

    private void validateExpiresAt(LocalDateTime expiresAt) {
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            throw new BusinessException(40079, HttpStatus.BAD_REQUEST, "expiresAt must be in the future");
        }
    }

    private String resolveContact(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred.trim() : trimToNull(fallback);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
