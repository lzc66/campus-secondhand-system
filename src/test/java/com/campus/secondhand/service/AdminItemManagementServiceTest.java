package com.campus.secondhand.service;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.admin.UpdateItemStatusRequest;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.AdminRoleCode;
import com.campus.secondhand.enums.ItemConditionLevel;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.enums.ItemTradeMode;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.ItemCategoryMapper;
import com.campus.secondhand.mapper.ItemImageMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.impl.AdminItemManagementServiceImpl;
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
class AdminItemManagementServiceTest {

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
    private AdminOperationLogMapper adminOperationLogMapper;

    @InjectMocks
    private AdminItemManagementServiceImpl adminItemManagementService;

    @Test
    void shouldOffShelfItemAndWriteAuditLog() {
        Item item = Item.builder()
                .itemId(101L)
                .sellerUserId(11L)
                .categoryId(2L)
                .title("iPad Air")
                .conditionLevel(ItemConditionLevel.LIGHTLY_USED)
                .tradeMode(ItemTradeMode.BOTH)
                .price(new BigDecimal("1999.00"))
                .stock(1)
                .negotiable(1)
                .status(ItemStatus.ON_SALE)
                .build();
        when(itemMapper.selectById(101L)).thenReturn(item);
        when(itemCategoryMapper.selectById(2L)).thenReturn(ItemCategory.builder().categoryId(2L).categoryName("鏁扮爜璁惧").build());
        when(userMapper.selectById(11L)).thenReturn(User.builder().userId(11L).studentNo("20240002").realName("Bob").email("bob@campus.local").phone("13900000000").build());
        when(itemImageMapper.selectList(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<com.campus.secondhand.entity.ItemImage>>any())).thenReturn(List.of());

        var response = adminItemManagementService.updateStatus(
                new AdminPrincipal(9001L, "admin1001", "Campus Admin", "admin@campus.local", AdminRoleCode.SUPER_ADMIN),
                101L,
                new UpdateItemStatusRequest("off_shelf", "policy review")
        );

        assertEquals("off_shelf", response.status());
        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).updateById(itemCaptor.capture());
        assertEquals(ItemStatus.OFF_SHELF, itemCaptor.getValue().getStatus());
        verify(adminOperationLogMapper).insert(any(com.campus.secondhand.entity.AdminOperationLog.class));
    }

    @Test
    void shouldRejectOnSaleWhenStockEmpty() {
        when(itemMapper.selectById(101L)).thenReturn(Item.builder()
                .itemId(101L)
                .sellerUserId(11L)
                .categoryId(2L)
                .stock(0)
                .status(ItemStatus.OFF_SHELF)
                .build());

        assertThrows(BusinessException.class, () -> adminItemManagementService.updateStatus(
                new AdminPrincipal(9001L, "admin1001", "Campus Admin", "admin@campus.local", AdminRoleCode.SUPER_ADMIN),
                101L,
                new UpdateItemStatusRequest("on_sale", null)
        ));
    }
}
