package com.campus.secondhand.service;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.user.SaveItemRequest;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.entity.ItemImage;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.mapper.ItemCategoryMapper;
import com.campus.secondhand.mapper.ItemImageMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.impl.UserItemServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserItemServiceTest {

    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ItemMapper itemMapper;
    @Mock
    private ItemImageMapper itemImageMapper;
    @Mock
    private ItemCategoryMapper itemCategoryMapper;
    @Mock
    private MediaFileMapper mediaFileMapper;

    @InjectMocks
    private UserItemServiceImpl userItemService;

    @Test
    void shouldCreateItem() {
        User seller = User.builder().userId(11L).phone("13800000000").qqNo("123").wechatNo("wx").dormitoryAddress("Dorm 101").build();
        ItemCategory category = ItemCategory.builder().categoryId(2L).categoryName("数码设备").isEnabled(1).build();
        MediaFile file = MediaFile.builder().fileId(8L).fileCategory("image").uploaderRole("user").uploaderRefId(11L).fileUrl("/uploads/item-images/a.png").originalName("a.png").build();
        when(userMapper.selectById(11L)).thenReturn(seller);
        when(itemCategoryMapper.selectById(2L)).thenReturn(category);
        when(mediaFileMapper.selectBatchIds(List.of(8L))).thenReturn(List.of(file));
        doAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            item.setItemId(101L);
            return 1;
        }).when(itemMapper).insert(any(Item.class));
        when(itemMapper.selectById(101L)).thenReturn(Item.builder()
                .itemId(101L)
                .sellerUserId(11L)
                .categoryId(2L)
                .title("iPad")
                .description("good")
                .conditionLevel(com.campus.secondhand.enums.ItemConditionLevel.ALMOST_NEW)
                .price(new BigDecimal("1999.00"))
                .stock(1)
                .tradeMode(com.campus.secondhand.enums.ItemTradeMode.BOTH)
                .negotiable(1)
                .contactPhone("13800000000")
                .contactQq("123")
                .contactWechat("wx")
                .pickupAddress("Dorm 101")
                .status(ItemStatus.ON_SALE)
                .viewCount(0)
                .commentCount(0)
                .build());
        when(itemImageMapper.selectList(any())).thenReturn(List.of(ItemImage.builder().itemId(101L).fileId(8L).sortOrder(0).isCover(1).build()));

        var response = userItemService.createItem(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", com.campus.secondhand.enums.UserAccountStatus.ACTIVE),
                new SaveItemRequest(2L, "iPad", null, null, "good", "almost_new", new BigDecimal("1999.00"), new BigDecimal("2999.00"), 1, "both", true, null, null, null, null, "on_sale", List.of(8L), 8L)
        );

        assertEquals(101L, response.itemId());
        assertEquals("iPad", response.title());
        verify(itemImageMapper).insert(any(ItemImage.class));
    }

    @Test
    void shouldRejectForeignImageFile() {
        User seller = User.builder().userId(11L).build();
        ItemCategory category = ItemCategory.builder().categoryId(2L).isEnabled(1).build();
        MediaFile file = MediaFile.builder().fileId(8L).fileCategory("image").uploaderRole("user").uploaderRefId(99L).build();
        when(userMapper.selectById(11L)).thenReturn(seller);
        when(itemCategoryMapper.selectById(2L)).thenReturn(category);
        when(mediaFileMapper.selectBatchIds(List.of(8L))).thenReturn(List.of(file));

        assertThrows(BusinessException.class, () -> userItemService.createItem(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", com.campus.secondhand.enums.UserAccountStatus.ACTIVE),
                new SaveItemRequest(2L, "iPad", null, null, "good", "almost_new", new BigDecimal("1999.00"), new BigDecimal("2999.00"), 1, "both", true, null, null, null, null, "on_sale", List.of(8L), 8L)
        ));
    }
}