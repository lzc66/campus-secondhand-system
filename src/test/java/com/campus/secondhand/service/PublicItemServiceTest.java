package com.campus.secondhand.service;

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
import com.campus.secondhand.service.impl.PublicItemServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicItemServiceTest {

    @Mock
    private ItemMapper itemMapper;
    @Mock
    private ItemCategoryMapper itemCategoryMapper;
    @Mock
    private ItemImageMapper itemImageMapper;
    @Mock
    private MediaFileMapper mediaFileMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RecommendationBehaviorService recommendationBehaviorService;

    @InjectMocks
    private PublicItemServiceImpl publicItemService;

    @Test
    void shouldListPublicItemsAndRecordSearch() {
        Item item = Item.builder()
                .itemId(1L)
                .categoryId(2L)
                .title("iPad Air")
                .brand("Apple")
                .model("Air 5")
                .conditionLevel(ItemConditionLevel.ALMOST_NEW)
                .price(new BigDecimal("1999.00"))
                .tradeMode(ItemTradeMode.BOTH)
                .negotiable(1)
                .status(ItemStatus.ON_SALE)
                .viewCount(10)
                .build();
        Page<Item> pageResult = new Page<>(1, 10, 1);
        pageResult.setRecords(List.of(item));
        when(itemMapper.selectPage(any(), any())).thenReturn(pageResult);
        when(itemCategoryMapper.selectById(2L)).thenReturn(ItemCategory.builder().categoryId(2L).categoryName("Digital Devices").build());
        when(itemImageMapper.selectList(any())).thenReturn(List.of(ItemImage.builder().itemId(1L).fileId(8L).sortOrder(0).isCover(1).build()));
        when(mediaFileMapper.selectById(8L)).thenReturn(MediaFile.builder().fileId(8L).fileUrl("/uploads/item-images/1.png").build());

        var response = publicItemService.listItems(11L, null, "iPad", null, null, null, null, null, "latest", 1, 10);

        assertEquals(1, response.total());
        assertEquals("iPad Air", response.records().get(0).title());
        verify(recommendationBehaviorService).recordSearch(11L, "iPad", null, null, null, "latest", "public_items");
    }

    @Test
    void shouldIncreaseViewCountWhenLoadingDetail() {
        Item item = Item.builder()
                .itemId(1L)
                .sellerUserId(11L)
                .categoryId(2L)
                .title("iPad Air")
                .brand("Apple")
                .model("Air 5")
                .description("good")
                .conditionLevel(ItemConditionLevel.ALMOST_NEW)
                .price(new BigDecimal("1999.00"))
                .stock(1)
                .tradeMode(ItemTradeMode.BOTH)
                .negotiable(1)
                .status(ItemStatus.ON_SALE)
                .viewCount(10)
                .commentCount(0)
                .build();
        when(itemMapper.selectById(1L)).thenReturn(item);
        when(itemCategoryMapper.selectById(2L)).thenReturn(ItemCategory.builder().categoryId(2L).categoryName("Digital Devices").build());
        when(userMapper.selectById(11L)).thenReturn(User.builder().userId(11L).realName("Alice").collegeName("Engineering").build());
        when(itemImageMapper.selectList(any())).thenReturn(List.of(ItemImage.builder().itemId(1L).fileId(8L).sortOrder(0).isCover(1).build()));
        when(mediaFileMapper.selectBatchIds(List.of(8L))).thenReturn(List.of(MediaFile.builder().fileId(8L).fileUrl("/uploads/item-images/1.png").originalName("1.png").build()));

        var response = publicItemService.getItemDetail(11L, 1L);

        assertEquals(11, response.viewCount());
        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).updateById(captor.capture());
        assertEquals(11, captor.getValue().getViewCount());
        verify(recommendationBehaviorService).recordItemView(11L, 1L, "public_item_detail");
    }

    @Test
    void shouldRejectInvalidPriceRange() {
        assertThrows(BusinessException.class, () -> publicItemService.listItems(null, null, null, null, new BigDecimal("10"), new BigDecimal("1"), null, null, null, 1, 10));
    }
}
