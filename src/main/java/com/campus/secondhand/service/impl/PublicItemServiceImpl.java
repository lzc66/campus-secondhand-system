package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.entity.ItemImage;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.ItemConditionLevel;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.enums.ItemTradeMode;
import com.campus.secondhand.mapper.ItemCategoryMapper;
import com.campus.secondhand.mapper.ItemImageMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.service.AdminDemoModeService;
import com.campus.secondhand.service.PublicItemService;
import com.campus.secondhand.service.RecommendationBehaviorService;
import com.campus.secondhand.vo.publicapi.PublicItemDetailResponse;
import com.campus.secondhand.vo.publicapi.PublicItemPageResponse;
import com.campus.secondhand.vo.publicapi.PublicItemSummaryResponse;
import com.campus.secondhand.vo.publicapi.PublicSellerResponse;
import com.campus.secondhand.vo.user.ItemImageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PublicItemServiceImpl implements PublicItemService {

    private final ItemMapper itemMapper;
    private final ItemCategoryMapper itemCategoryMapper;
    private final ItemImageMapper itemImageMapper;
    private final MediaFileMapper mediaFileMapper;
    private final UserMapper userMapper;
    private final RecommendationBehaviorService recommendationBehaviorService;
    private final AdminDemoModeService adminDemoModeService;

    public PublicItemServiceImpl(ItemMapper itemMapper,
                                 ItemCategoryMapper itemCategoryMapper,
                                 ItemImageMapper itemImageMapper,
                                 MediaFileMapper mediaFileMapper,
                                 UserMapper userMapper,
                                 RecommendationBehaviorService recommendationBehaviorService,
                                 AdminDemoModeService adminDemoModeService) {
        this.itemMapper = itemMapper;
        this.itemCategoryMapper = itemCategoryMapper;
        this.itemImageMapper = itemImageMapper;
        this.mediaFileMapper = mediaFileMapper;
        this.userMapper = userMapper;
        this.recommendationBehaviorService = recommendationBehaviorService;
        this.adminDemoModeService = adminDemoModeService;
    }

    @Override
    public PublicItemPageResponse listItems(Long userId,
                                            Long categoryId,
                                            String keyword,
                                            String brand,
                                            BigDecimal priceMin,
                                            BigDecimal priceMax,
                                            String conditionLevel,
                                            String tradeMode,
                                            String sortBy,
                                            long page,
                                            long size) {
        validatePriceRange(priceMin, priceMax);
        ItemConditionLevel conditionFilter = parseConditionLevel(conditionLevel);
        ItemTradeMode tradeModeFilter = parseTradeMode(tradeMode);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedBrand = StringUtils.hasText(brand) ? brand.trim() : null;

        Page<Item> queryPage = new Page<>(Math.max(page, 1), Math.max(size, 1));
        LambdaQueryWrapper<Item> wrapper = Wrappers.lambdaQuery(Item.class)
                .eq(Item::getStatus, ItemStatus.ON_SALE)
                .isNull(Item::getDeletedAt)
                .and(!adminDemoModeService.isDemoModeEnabled(), q -> q
                        .notLike(Item::getTitle, "[演示]%")
                        .notLike(Item::getTitle, "[Demo]%"))
                .eq(categoryId != null, Item::getCategoryId, categoryId)
                .eq(conditionFilter != null, Item::getConditionLevel, conditionFilter)
                .eq(tradeModeFilter != null, Item::getTradeMode, tradeModeFilter)
                .eq(StringUtils.hasText(normalizedBrand), Item::getBrand, normalizedBrand)
                .ge(priceMin != null, Item::getPrice, priceMin)
                .le(priceMax != null, Item::getPrice, priceMax)
                .and(StringUtils.hasText(normalizedKeyword), q -> q
                        .like(Item::getTitle, normalizedKeyword)
                        .or().like(Item::getBrand, normalizedKeyword)
                        .or().like(Item::getModel, normalizedKeyword)
                        .or().like(Item::getDescription, normalizedKeyword));
        applySort(wrapper, sortBy);
        Page<Item> result = itemMapper.selectPage(queryPage, wrapper);
        List<PublicItemSummaryResponse> records = buildSummaryResponses(result.getRecords());
        if (shouldRecordSearch(userId, categoryId, normalizedKeyword, normalizedBrand, priceMin, priceMax, conditionLevel, tradeMode)) {
            recommendationBehaviorService.recordSearch(userId, normalizedKeyword, categoryId, priceMin, priceMax, sortBy, "public_items");
        }
        return new PublicItemPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    @Transactional
    public PublicItemDetailResponse getItemDetail(Long userId, Long itemId) {
        Item item = getRequiredPublicItem(itemId);
        item.setViewCount(item.getViewCount() == null ? 1 : item.getViewCount() + 1);
        itemMapper.updateById(item);
        recommendationBehaviorService.recordItemView(userId, itemId, "public_item_detail");
        return buildDetailResponse(item);
    }

    /**
     * 批量组装列表响应:分类与封面图一次性批量预取,避免每条商品记录产生 3 次查询的 N+1。
     */
    private List<PublicItemSummaryResponse> buildSummaryResponses(List<Item> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> categoryIds = items.stream().map(Item::getCategoryId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ItemCategory> categoryMap = categoryIds.isEmpty() ? Map.of()
                : itemCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(ItemCategory::getCategoryId, category -> category, (a, b) -> a, LinkedHashMap::new));
        List<Long> itemIds = items.stream().map(Item::getItemId).toList();
        Map<Long, ItemImage> coverImageMap = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                        .in(ItemImage::getItemId, itemIds)
                        .orderByAsc(ItemImage::getSortOrder)).stream()
                .collect(Collectors.toMap(ItemImage::getItemId, image -> image,
                        (a, b) -> Objects.equals(a.getIsCover(), 1) ? a : b, LinkedHashMap::new));
        List<Long> coverFileIds = coverImageMap.values().stream().map(ItemImage::getFileId).filter(Objects::nonNull).distinct().toList();
        Map<Long, MediaFile> fileMap = coverFileIds.isEmpty() ? Map.of()
                : mediaFileMapper.selectBatchIds(coverFileIds).stream()
                .collect(Collectors.toMap(MediaFile::getFileId, file -> file, (a, b) -> a, LinkedHashMap::new));
        return items.stream().map(item -> {
            ItemCategory category = categoryMap.get(item.getCategoryId());
            ItemImage cover = coverImageMap.get(item.getItemId());
            MediaFile coverFile = cover == null ? null : fileMap.get(cover.getFileId());
            return new PublicItemSummaryResponse(
                    item.getItemId(),
                    item.getCategoryId(),
                    category == null ? null : category.getCategoryName(),
                    item.getTitle(),
                    item.getBrand(),
                    item.getModel(),
                    item.getConditionLevel().getValue(),
                    item.getPrice(),
                    item.getTradeMode().getValue(),
                    Objects.equals(item.getNegotiable(), 1),
                    coverFile == null ? null : coverFile.getFileUrl(),
                    item.getViewCount(),
                    item.getPublishedAt()
            );
        }).toList();
    }

    private PublicItemDetailResponse buildDetailResponse(Item item) {
        ItemCategory category = itemCategoryMapper.selectById(item.getCategoryId());
        User seller = userMapper.selectById(item.getSellerUserId());
        MediaFile avatar = seller == null || seller.getAvatarFileId() == null ? null : mediaFileMapper.selectById(seller.getAvatarFileId());
        List<ItemImage> itemImages = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, item.getItemId())
                .orderByAsc(ItemImage::getSortOrder));
        Map<Long, MediaFile> fileMap = mediaFileMapper.selectBatchIds(itemImages.stream().map(ItemImage::getFileId).toList()).stream()
                .collect(Collectors.toMap(MediaFile::getFileId, media -> media, (a, b) -> a, LinkedHashMap::new));
        List<ItemImageResponse> images = itemImages.stream()
                .sorted(Comparator.comparing(ItemImage::getSortOrder))
                .map(itemImage -> {
                    MediaFile file = fileMap.get(itemImage.getFileId());
                    return new ItemImageResponse(
                            itemImage.getFileId(),
                            file == null ? null : file.getFileUrl(),
                            file == null ? null : file.getOriginalName(),
                            itemImage.getSortOrder(),
                            Objects.equals(itemImage.getIsCover(), 1)
                    );
                })
                .toList();
        PublicSellerResponse sellerResponse = seller == null ? null : new PublicSellerResponse(
                seller.getUserId(),
                seller.getRealName(),
                avatar == null ? null : avatar.getFileUrl(),
                seller.getCollegeName(),
                seller.getMajorName(),
                seller.getClassName()
        );
        return new PublicItemDetailResponse(
                item.getItemId(),
                item.getCategoryId(),
                category == null ? null : category.getCategoryName(),
                item.getTitle(),
                item.getBrand(),
                item.getModel(),
                item.getDescription(),
                item.getConditionLevel().getValue(),
                item.getPrice(),
                item.getOriginalPrice(),
                item.getStock(),
                item.getTradeMode().getValue(),
                Objects.equals(item.getNegotiable(), 1),
                item.getContactPhone(),
                item.getContactQq(),
                item.getContactWechat(),
                item.getPickupAddress(),
                item.getViewCount(),
                item.getCommentCount(),
                item.getPublishedAt(),
                item.getCreatedAt(),
                sellerResponse,
                images
        );
    }

    private boolean shouldRecordSearch(Long userId,
                                       Long categoryId,
                                       String keyword,
                                       String brand,
                                       BigDecimal priceMin,
                                       BigDecimal priceMax,
                                       String conditionLevel,
                                       String tradeMode) {
        return userId != null && (categoryId != null
                || StringUtils.hasText(keyword)
                || StringUtils.hasText(brand)
                || priceMin != null
                || priceMax != null
                || StringUtils.hasText(conditionLevel)
                || StringUtils.hasText(tradeMode));
    }

    private String resolveCoverImageUrl(Long itemId) {
        List<ItemImage> images = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, itemId)
                .orderByAsc(ItemImage::getSortOrder));
        ItemImage cover = images.stream().filter(img -> Objects.equals(img.getIsCover(), 1)).findFirst().orElse(images.isEmpty() ? null : images.get(0));
        if (cover == null) {
            return null;
        }
        MediaFile mediaFile = mediaFileMapper.selectById(cover.getFileId());
        return mediaFile == null ? null : mediaFile.getFileUrl();
    }

    private Item getRequiredPublicItem(Long itemId) {
        Item item = itemMapper.selectById(itemId);
        // 软删除的商品(status 与 deleted_at 双账本)不得公开可见:完成/取消订单等路径可能只改回 status,
        // 因此这里必须同时校验 deleted_at。
        if (item == null || item.getStatus() != ItemStatus.ON_SALE || item.getDeletedAt() != null) {
            throw new BusinessException(40450, HttpStatus.NOT_FOUND, "Item not found");
        }
        // 演示模式关闭时,演示商品对外表现为不存在
        if (!adminDemoModeService.isDemoModeEnabled() && isDemoPrefixedTitle(item.getTitle())) {
            throw new BusinessException(40450, HttpStatus.NOT_FOUND, "Item not found");
        }
        return item;
    }

    private boolean isDemoPrefixedTitle(String title) {
        return title != null && (title.startsWith("[演示]") || title.startsWith("[Demo]"));
    }

    private void applySort(LambdaQueryWrapper<Item> wrapper, String sortBy) {
        String normalized = StringUtils.hasText(sortBy) ? sortBy.trim().toLowerCase(Locale.ROOT) : "latest";
        switch (normalized) {
            case "price_asc" -> wrapper.orderByAsc(Item::getPrice).orderByDesc(Item::getPublishedAt);
            case "price_desc" -> wrapper.orderByDesc(Item::getPrice).orderByDesc(Item::getPublishedAt);
            case "popular" -> wrapper.orderByDesc(Item::getViewCount).orderByDesc(Item::getPublishedAt);
            default -> wrapper.orderByDesc(Item::getPublishedAt).orderByDesc(Item::getCreatedAt);
        }
    }

    private void validatePriceRange(BigDecimal priceMin, BigDecimal priceMax) {
        if (priceMin != null && priceMin.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(40060, HttpStatus.BAD_REQUEST, "priceMin must be greater than or equal to 0");
        }
        if (priceMax != null && priceMax.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(40061, HttpStatus.BAD_REQUEST, "priceMax must be greater than or equal to 0");
        }
        if (priceMin != null && priceMax != null && priceMax.compareTo(priceMin) < 0) {
            throw new BusinessException(40062, HttpStatus.BAD_REQUEST, "priceMax must be greater than or equal to priceMin");
        }
    }

    private ItemConditionLevel parseConditionLevel(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "new" -> ItemConditionLevel.NEW;
            case "almost_new" -> ItemConditionLevel.ALMOST_NEW;
            case "lightly_used" -> ItemConditionLevel.LIGHTLY_USED;
            case "used" -> ItemConditionLevel.USED;
            case "well_used" -> ItemConditionLevel.WELL_USED;
            default -> throw new BusinessException(40063, HttpStatus.BAD_REQUEST, "conditionLevel is invalid");
        };
    }

    private ItemTradeMode parseTradeMode(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "offline" -> ItemTradeMode.OFFLINE;
            case "online" -> ItemTradeMode.ONLINE;
            case "both" -> ItemTradeMode.BOTH;
            default -> throw new BusinessException(40064, HttpStatus.BAD_REQUEST, "tradeMode is invalid");
        };
    }
}
