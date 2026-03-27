package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.admin.UpdateItemStatusRequest;
import com.campus.secondhand.entity.AdminOperationLog;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.entity.ItemImage;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.ItemCategoryMapper;
import com.campus.secondhand.mapper.ItemImageMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.AdminItemManagementService;
import com.campus.secondhand.vo.admin.AdminItemDetailResponse;
import com.campus.secondhand.vo.admin.AdminItemPageResponse;
import com.campus.secondhand.vo.admin.AdminItemSellerResponse;
import com.campus.secondhand.vo.admin.AdminItemSummaryResponse;
import com.campus.secondhand.vo.user.ItemImageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminItemManagementServiceImpl implements AdminItemManagementService {

    private final ItemMapper itemMapper;
    private final ItemCategoryMapper itemCategoryMapper;
    private final ItemImageMapper itemImageMapper;
    private final MediaFileMapper mediaFileMapper;
    private final UserMapper userMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;

    public AdminItemManagementServiceImpl(ItemMapper itemMapper,
                                          ItemCategoryMapper itemCategoryMapper,
                                          ItemImageMapper itemImageMapper,
                                          MediaFileMapper mediaFileMapper,
                                          UserMapper userMapper,
                                          AdminOperationLogMapper adminOperationLogMapper) {
        this.itemMapper = itemMapper;
        this.itemCategoryMapper = itemCategoryMapper;
        this.itemImageMapper = itemImageMapper;
        this.mediaFileMapper = mediaFileMapper;
        this.userMapper = userMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
    }

    @Override
    public AdminItemPageResponse list(String status, Long categoryId, String keyword, String sellerStudentNo, long page, long size) {
        ItemStatus statusFilter = parseListStatus(status);
        Long sellerUserId = resolveSellerUserId(sellerStudentNo);
        if (StringUtils.hasText(sellerStudentNo) && sellerUserId == null) {
            return new AdminItemPageResponse(Math.max(page, 1), Math.max(size, 1), 0, List.of());
        }
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Page<Item> queryPage = new Page<>(Math.max(page, 1), Math.max(size, 1));
        Page<Item> result = itemMapper.selectPage(queryPage, new LambdaQueryWrapper<Item>()
                .eq(statusFilter != null, Item::getStatus, statusFilter)
                .eq(categoryId != null, Item::getCategoryId, categoryId)
                .eq(sellerUserId != null, Item::getSellerUserId, sellerUserId)
                .and(StringUtils.hasText(trimmedKeyword), q -> q
                        .like(Item::getTitle, trimmedKeyword)
                        .or().like(Item::getBrand, trimmedKeyword)
                        .or().like(Item::getModel, trimmedKeyword)
                        .or().like(Item::getDescription, trimmedKeyword))
                .orderByDesc(Item::getCreatedAt));
        Map<Long, User> userMap = loadUsers(result.getRecords().stream().map(Item::getSellerUserId).distinct().toList());
        Map<Long, ItemCategory> categoryMap = loadCategories(result.getRecords().stream().map(Item::getCategoryId).distinct().toList());
        List<AdminItemSummaryResponse> records = result.getRecords().stream()
                .map(item -> toSummary(item, userMap.get(item.getSellerUserId()), categoryMap.get(item.getCategoryId())))
                .toList();
        return new AdminItemPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public AdminItemDetailResponse detail(Long itemId) {
        return toDetail(getRequiredItem(itemId));
    }

    @Override
    @Transactional
    public AdminItemDetailResponse updateStatus(AdminPrincipal principal, Long itemId, UpdateItemStatusRequest request) {
        Item item = getRequiredItem(itemId);
        ItemStatus targetStatus = parseMutableStatus(request.itemStatus());
        if (item.getStatus() == ItemStatus.DELETED && targetStatus != ItemStatus.DELETED) {
            throw new BusinessException(40940, HttpStatus.CONFLICT, "Deleted item cannot be restored");
        }
        if (targetStatus == ItemStatus.ON_SALE && (item.getStock() == null || item.getStock() <= 0)) {
            throw new BusinessException(40941, HttpStatus.CONFLICT, "Item with empty stock cannot be set to on_sale");
        }
        item.setStatus(targetStatus);
        if (targetStatus == ItemStatus.DELETED) {
            item.setDeletedAt(LocalDateTime.now());
        } else {
            item.setDeletedAt(null);
        }
        if (targetStatus == ItemStatus.ON_SALE && item.getPublishedAt() == null) {
            item.setPublishedAt(LocalDateTime.now());
        }
        item.setSoldAt(null);
        itemMapper.updateById(item);
        logOperation(principal.getAdminId(), item.getItemId(), "update_status", "{\"itemStatus\":\"" + targetStatus.getValue() + "\"}");
        return toDetail(item);
    }

    private Item getRequiredItem(Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(40440, HttpStatus.NOT_FOUND, "Item not found");
        }
        return item;
    }

    private Long resolveSellerUserId(String sellerStudentNo) {
        if (!StringUtils.hasText(sellerStudentNo)) {
            return null;
        }
        User seller = userMapper.selectByStudentNo(sellerStudentNo.trim());
        return seller == null ? null : seller.getUserId();
    }

    private Map<Long, User> loadUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, user -> user, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, ItemCategory> loadCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        return itemCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(ItemCategory::getCategoryId, category -> category, (a, b) -> a, LinkedHashMap::new));
    }

    private AdminItemSummaryResponse toSummary(Item item, User seller, ItemCategory category) {
        return new AdminItemSummaryResponse(
                item.getItemId(),
                item.getSellerUserId(),
                seller == null ? null : seller.getStudentNo(),
                seller == null ? null : seller.getRealName(),
                item.getCategoryId(),
                category == null ? null : category.getCategoryName(),
                item.getTitle(),
                item.getPrice(),
                item.getStock(),
                item.getStatus().getValue(),
                item.getViewCount(),
                item.getCommentCount(),
                item.getPublishedAt(),
                item.getCreatedAt()
        );
    }

    private AdminItemDetailResponse toDetail(Item item) {
        ItemCategory category = itemCategoryMapper.selectById(item.getCategoryId());
        User seller = userMapper.selectById(item.getSellerUserId());
        List<ItemImage> itemImages = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, item.getItemId())
                .orderByAsc(ItemImage::getSortOrder));
        Map<Long, MediaFile> fileMap = itemImages.isEmpty() ? Map.of() : mediaFileMapper.selectBatchIds(itemImages.stream().map(ItemImage::getFileId).toList()).stream()
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
        return new AdminItemDetailResponse(
                item.getItemId(),
                item.getCategoryId(),
                category == null ? null : category.getCategoryName(),
                item.getTitle(),
                item.getBrand(),
                item.getModel(),
                item.getDescription(),
                item.getConditionLevel() == null ? null : item.getConditionLevel().getValue(),
                item.getPrice(),
                item.getOriginalPrice(),
                item.getStock(),
                item.getTradeMode() == null ? null : item.getTradeMode().getValue(),
                Objects.equals(item.getNegotiable(), 1),
                item.getContactPhone(),
                item.getContactQq(),
                item.getContactWechat(),
                item.getPickupAddress(),
                item.getStatus().getValue(),
                item.getViewCount(),
                item.getCommentCount(),
                item.getPublishedAt(),
                item.getSoldAt(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                seller == null ? null : new AdminItemSellerResponse(
                        seller.getUserId(),
                        seller.getStudentNo(),
                        seller.getRealName(),
                        seller.getEmail(),
                        seller.getPhone()
                ),
                images
        );
    }

    private ItemStatus parseListStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "draft" -> ItemStatus.DRAFT;
            case "on_sale" -> ItemStatus.ON_SALE;
            case "reserved" -> ItemStatus.RESERVED;
            case "sold" -> ItemStatus.SOLD;
            case "off_shelf" -> ItemStatus.OFF_SHELF;
            case "deleted" -> ItemStatus.DELETED;
            default -> throw new BusinessException(40040, HttpStatus.BAD_REQUEST, "status filter is invalid");
        };
    }

    private ItemStatus parseMutableStatus(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(40041, HttpStatus.BAD_REQUEST, "itemStatus is required");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "on_sale" -> ItemStatus.ON_SALE;
            case "off_shelf" -> ItemStatus.OFF_SHELF;
            case "deleted" -> ItemStatus.DELETED;
            default -> throw new BusinessException(40042, HttpStatus.BAD_REQUEST, "itemStatus must be on_sale, off_shelf, or deleted");
        };
    }

    private void logOperation(Long adminId, Long itemId, String operationType, String operationDetail) {
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminId(adminId)
                .targetType("item")
                .targetId(itemId)
                .operationType(operationType)
                .operationDetail(operationDetail)
                .ipAddress(null)
                .build());
    }
}
