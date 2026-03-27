package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.user.SaveItemRequest;
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
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.FileStorageService;
import com.campus.secondhand.service.UserItemService;
import com.campus.secondhand.vo.common.MediaFileResponse;
import com.campus.secondhand.vo.user.ItemImageResponse;
import com.campus.secondhand.vo.user.UserItemDetailResponse;
import com.campus.secondhand.vo.user.UserItemPageResponse;
import com.campus.secondhand.vo.user.UserItemSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserItemServiceImpl implements UserItemService {

    private final FileStorageService fileStorageService;
    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final ItemCategoryMapper itemCategoryMapper;
    private final MediaFileMapper mediaFileMapper;

    public UserItemServiceImpl(FileStorageService fileStorageService,
                               UserMapper userMapper,
                               ItemMapper itemMapper,
                               ItemImageMapper itemImageMapper,
                               ItemCategoryMapper itemCategoryMapper,
                               MediaFileMapper mediaFileMapper) {
        this.fileStorageService = fileStorageService;
        this.userMapper = userMapper;
        this.itemMapper = itemMapper;
        this.itemImageMapper = itemImageMapper;
        this.itemCategoryMapper = itemCategoryMapper;
        this.mediaFileMapper = mediaFileMapper;
    }

    @Override
    public MediaFileResponse uploadItemImage(UserPrincipal principal, MultipartFile file) {
        return fileStorageService.storeItemImage(principal.getUserId(), file);
    }

    @Override
    @Transactional
    public UserItemDetailResponse createItem(UserPrincipal principal, SaveItemRequest request) {
        User seller = getRequiredSeller(principal.getUserId());
        ItemCategory category = getRequiredCategory(request.categoryId());
        List<MediaFile> imageFiles = validateImageFiles(principal.getUserId(), request.imageFileIds());
        Long coverFileId = resolveCoverFileId(request.coverImageFileId(), imageFiles);
        Item item = Item.builder()
                .sellerUserId(principal.getUserId())
                .categoryId(category.getCategoryId())
                .title(request.title().trim())
                .brand(trimToNull(request.brand()))
                .model(trimToNull(request.model()))
                .description(request.description().trim())
                .conditionLevel(parseConditionLevel(request.conditionLevel()))
                .price(request.price())
                .originalPrice(validateOriginalPrice(request.originalPrice(), request.price()))
                .stock(request.stock())
                .tradeMode(parseTradeMode(request.tradeMode()))
                .negotiable(Boolean.TRUE.equals(request.negotiable()) ? 1 : 0)
                .contactPhone(resolveContact(request.contactPhone(), seller.getPhone()))
                .contactQq(resolveContact(request.contactQq(), seller.getQqNo()))
                .contactWechat(resolveContact(request.contactWechat(), seller.getWechatNo()))
                .pickupAddress(resolveContact(request.pickupAddress(), seller.getDormitoryAddress()))
                .status(parseEditableStatus(request.status()))
                .publishedAt(parseEditableStatus(request.status()) == ItemStatus.ON_SALE ? LocalDateTime.now() : null)
                .viewCount(0)
                .commentCount(0)
                .build();
        itemMapper.insert(item);
        replaceItemImages(item.getItemId(), imageFiles, coverFileId);
        return buildDetailResponse(itemMapper.selectById(item.getItemId()));
    }

    @Override
    @Transactional
    public UserItemDetailResponse updateItem(UserPrincipal principal, Long itemId, SaveItemRequest request) {
        Item item = getOwnedItem(principal.getUserId(), itemId);
        if (item.getStatus() == ItemStatus.DELETED) {
            throw new BusinessException(40940, HttpStatus.CONFLICT, "Deleted item cannot be updated");
        }
        User seller = getRequiredSeller(principal.getUserId());
        ItemCategory category = getRequiredCategory(request.categoryId());
        List<MediaFile> imageFiles = validateImageFiles(principal.getUserId(), request.imageFileIds());
        Long coverFileId = resolveCoverFileId(request.coverImageFileId(), imageFiles);
        ItemStatus newStatus = parseEditableStatus(request.status());

        item.setCategoryId(category.getCategoryId());
        item.setTitle(request.title().trim());
        item.setBrand(trimToNull(request.brand()));
        item.setModel(trimToNull(request.model()));
        item.setDescription(request.description().trim());
        item.setConditionLevel(parseConditionLevel(request.conditionLevel()));
        item.setPrice(request.price());
        item.setOriginalPrice(validateOriginalPrice(request.originalPrice(), request.price()));
        item.setStock(request.stock());
        item.setTradeMode(parseTradeMode(request.tradeMode()));
        item.setNegotiable(Boolean.TRUE.equals(request.negotiable()) ? 1 : 0);
        item.setContactPhone(resolveContact(request.contactPhone(), seller.getPhone()));
        item.setContactQq(resolveContact(request.contactQq(), seller.getQqNo()));
        item.setContactWechat(resolveContact(request.contactWechat(), seller.getWechatNo()));
        item.setPickupAddress(resolveContact(request.pickupAddress(), seller.getDormitoryAddress()));
        if (item.getPublishedAt() == null && newStatus == ItemStatus.ON_SALE) {
            item.setPublishedAt(LocalDateTime.now());
        }
        item.setStatus(newStatus);
        itemMapper.updateById(item);
        replaceItemImages(item.getItemId(), imageFiles, coverFileId);
        return buildDetailResponse(itemMapper.selectById(item.getItemId()));
    }

    @Override
    public UserItemPageResponse listMyItems(UserPrincipal principal, String status, long page, long size) {
        ItemStatus statusFilter = parseListStatus(status);
        Page<Item> queryPage = new Page<>(Math.max(page, 1), Math.max(size, 1));
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getSellerUserId, principal.getUserId())
                .eq(statusFilter != null, Item::getStatus, statusFilter)
                .ne(statusFilter == null, Item::getStatus, ItemStatus.DELETED)
                .orderByDesc(Item::getCreatedAt);
        Page<Item> result = itemMapper.selectPage(queryPage, wrapper);
        List<UserItemSummaryResponse> records = result.getRecords().stream()
                .map(this::buildSummaryResponse)
                .toList();
        return new UserItemPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public UserItemDetailResponse getMyItem(UserPrincipal principal, Long itemId) {
        return buildDetailResponse(getOwnedItem(principal.getUserId(), itemId));
    }

    @Override
    @Transactional
    public void deleteItem(UserPrincipal principal, Long itemId) {
        Item item = getOwnedItem(principal.getUserId(), itemId);
        if (item.getStatus() == ItemStatus.DELETED) {
            return;
        }
        item.setStatus(ItemStatus.DELETED);
        item.setDeletedAt(LocalDateTime.now());
        itemMapper.updateById(item);
    }

    private UserItemSummaryResponse buildSummaryResponse(Item item) {
        ItemCategory category = itemCategoryMapper.selectById(item.getCategoryId());
        String coverImageUrl = null;
        List<ItemImage> images = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, item.getItemId())
                .orderByAsc(ItemImage::getSortOrder));
        ItemImage cover = images.stream().filter(img -> Objects.equals(img.getIsCover(), 1)).findFirst().orElse(images.isEmpty() ? null : images.get(0));
        if (cover != null) {
            MediaFile mediaFile = mediaFileMapper.selectById(cover.getFileId());
            coverImageUrl = mediaFile == null ? null : mediaFile.getFileUrl();
        }
        return new UserItemSummaryResponse(
                item.getItemId(),
                item.getCategoryId(),
                category == null ? null : category.getCategoryName(),
                item.getTitle(),
                item.getPrice(),
                item.getStock(),
                item.getStatus().getValue(),
                coverImageUrl,
                item.getPublishedAt(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private UserItemDetailResponse buildDetailResponse(Item item) {
        ItemCategory category = itemCategoryMapper.selectById(item.getCategoryId());
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
        return new UserItemDetailResponse(
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
                item.getStatus().getValue(),
                item.getViewCount(),
                item.getCommentCount(),
                item.getPublishedAt(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                images
        );
    }

    private void replaceItemImages(Long itemId, List<MediaFile> imageFiles, Long coverFileId) {
        itemImageMapper.delete(new LambdaQueryWrapper<ItemImage>().eq(ItemImage::getItemId, itemId));
        for (int i = 0; i < imageFiles.size(); i++) {
            MediaFile file = imageFiles.get(i);
            itemImageMapper.insert(ItemImage.builder()
                    .itemId(itemId)
                    .fileId(file.getFileId())
                    .sortOrder(i)
                    .isCover(Objects.equals(file.getFileId(), coverFileId) ? 1 : 0)
                    .build());
        }
    }

    private Long resolveCoverFileId(Long coverImageFileId, List<MediaFile> imageFiles) {
        if (coverImageFileId == null) {
            return imageFiles.get(0).getFileId();
        }
        boolean exists = imageFiles.stream().anyMatch(file -> Objects.equals(file.getFileId(), coverImageFileId));
        if (!exists) {
            throw new BusinessException(40040, HttpStatus.BAD_REQUEST, "coverImageFileId must be included in imageFileIds");
        }
        return coverImageFileId;
    }

    private List<MediaFile> validateImageFiles(Long userId, List<Long> imageFileIds) {
        List<MediaFile> files = mediaFileMapper.selectBatchIds(imageFileIds);
        if (files.size() != imageFileIds.size()) {
            throw new BusinessException(40041, HttpStatus.BAD_REQUEST, "Some imageFileIds do not exist");
        }
        Map<Long, MediaFile> fileMap = files.stream().collect(Collectors.toMap(MediaFile::getFileId, file -> file));
        return imageFileIds.stream().map(fileId -> {
            MediaFile file = fileMap.get(fileId);
            if (!"image".equalsIgnoreCase(file.getFileCategory())) {
                throw new BusinessException(40042, HttpStatus.BAD_REQUEST, "All item images must be image files");
            }
            if (!"user".equalsIgnoreCase(file.getUploaderRole()) || !Objects.equals(file.getUploaderRefId(), userId)) {
                throw new BusinessException(40340, HttpStatus.FORBIDDEN, "Image file does not belong to current user");
            }
            return file;
        }).toList();
    }

    private User getRequiredSeller(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40420, HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }

    private ItemCategory getRequiredCategory(Long categoryId) {
        ItemCategory category = itemCategoryMapper.selectById(categoryId);
        if (category == null || !Objects.equals(category.getIsEnabled(), 1)) {
            throw new BusinessException(40043, HttpStatus.BAD_REQUEST, "categoryId is invalid");
        }
        return category;
    }

    private Item getOwnedItem(Long userId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null || !Objects.equals(item.getSellerUserId(), userId)) {
            throw new BusinessException(40440, HttpStatus.NOT_FOUND, "Item not found");
        }
        return item;
    }

    private ItemConditionLevel parseConditionLevel(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "new" -> ItemConditionLevel.NEW;
            case "almost_new" -> ItemConditionLevel.ALMOST_NEW;
            case "lightly_used" -> ItemConditionLevel.LIGHTLY_USED;
            case "used" -> ItemConditionLevel.USED;
            case "well_used" -> ItemConditionLevel.WELL_USED;
            default -> throw new BusinessException(40044, HttpStatus.BAD_REQUEST, "conditionLevel is invalid");
        };
    }

    private ItemTradeMode parseTradeMode(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "offline" -> ItemTradeMode.OFFLINE;
            case "online" -> ItemTradeMode.ONLINE;
            case "both" -> ItemTradeMode.BOTH;
            default -> throw new BusinessException(40045, HttpStatus.BAD_REQUEST, "tradeMode is invalid");
        };
    }

    private ItemStatus parseEditableStatus(String value) {
        String normalized = StringUtils.hasText(value) ? normalize(value) : "on_sale";
        return switch (normalized) {
            case "draft" -> ItemStatus.DRAFT;
            case "on_sale" -> ItemStatus.ON_SALE;
            case "off_shelf" -> ItemStatus.OFF_SHELF;
            default -> throw new BusinessException(40046, HttpStatus.BAD_REQUEST, "status must be draft, on_sale, or off_shelf");
        };
    }

    private ItemStatus parseListStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = normalize(value);
        return switch (normalized) {
            case "draft" -> ItemStatus.DRAFT;
            case "on_sale" -> ItemStatus.ON_SALE;
            case "reserved" -> ItemStatus.RESERVED;
            case "sold" -> ItemStatus.SOLD;
            case "off_shelf" -> ItemStatus.OFF_SHELF;
            case "deleted" -> ItemStatus.DELETED;
            default -> throw new BusinessException(40047, HttpStatus.BAD_REQUEST, "status filter is invalid");
        };
    }

    private BigDecimal validateOriginalPrice(BigDecimal originalPrice, BigDecimal price) {
        if (originalPrice != null && originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(40048, HttpStatus.BAD_REQUEST, "originalPrice must be greater than 0");
        }
        if (originalPrice != null && originalPrice.compareTo(price) < 0) {
            throw new BusinessException(40049, HttpStatus.BAD_REQUEST, "originalPrice must be greater than or equal to price");
        }
        return originalPrice;
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