package com.campus.secondhand.service;

import com.campus.secondhand.dto.admin.AdminOrderActionRequest;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.OrderItem;
import com.campus.secondhand.entity.OrderStatusLog;
import com.campus.secondhand.entity.TradeOrder;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.AdminRoleCode;
import com.campus.secondhand.enums.CancelledByType;
import com.campus.secondhand.enums.DeliveryType;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.enums.OrderStatus;
import com.campus.secondhand.enums.OrderType;
import com.campus.secondhand.enums.PaymentMethod;
import com.campus.secondhand.enums.PaymentStatus;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.OrderItemMapper;
import com.campus.secondhand.mapper.OrderStatusLogMapper;
import com.campus.secondhand.mapper.TradeOrderMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.impl.AdminOrderManagementServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderManagementServiceTest {

    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private OrderStatusLogMapper orderStatusLogMapper;
    @Mock
    private ItemMapper itemMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AdminOperationLogMapper adminOperationLogMapper;

    @InjectMocks
    private AdminOrderManagementServiceImpl adminOrderManagementService;

    @Test
    void shouldCancelOrderAndRestoreStock() {
        TradeOrder order = TradeOrder.builder()
                .orderId(1L)
                .orderNo("ORD1")
                .buyerUserId(11L)
                .sellerUserId(22L)
                .orderType(OrderType.ONLINE_COD)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.UNPAID)
                .orderStatus(OrderStatus.PENDING_CONFIRM)
                .deliveryType(DeliveryType.DORM_DELIVERY)
                .receiverName("Alice")
                .receiverPhone("13800000000")
                .deliveryAddress("Dorm 101")
                .totalAmount(new BigDecimal("1999.00"))
                .build();
        when(tradeOrderMapper.selectById(1L)).thenReturn(order);
        when(orderItemMapper.selectList(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<com.campus.secondhand.entity.OrderItem>>any())).thenReturn(List.of(OrderItem.builder()
                .orderItemId(9L)
                .orderId(1L)
                .itemId(101L)
                .itemTitleSnapshot("iPad Air")
                .itemPriceSnapshot(new BigDecimal("1999.00"))
                .quantity(1)
                .subtotalAmount(new BigDecimal("1999.00"))
                .build()));
        when(itemMapper.selectById(101L)).thenReturn(Item.builder().itemId(101L).stock(0).status(ItemStatus.RESERVED).build());
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                User.builder().userId(11L).studentNo("20240001").realName("Alice").phone("13800000000").build(),
                User.builder().userId(22L).studentNo("20240002").realName("Bob").phone("13900000000").build()
        ));
        when(orderStatusLogMapper.selectList(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<com.campus.secondhand.entity.OrderStatusLog>>any())).thenReturn(List.of());

        var response = adminOrderManagementService.cancel(
                new AdminPrincipal(9001L, "admin1001", "Campus Admin", "admin@campus.local", AdminRoleCode.SUPER_ADMIN),
                1L,
                new AdminOrderActionRequest("risk control")
        );

        assertEquals("cancelled", response.orderStatus());
        assertEquals("admin", response.cancelledBy());
        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).updateById(itemCaptor.capture());
        assertEquals(1, itemCaptor.getValue().getStock());
        assertEquals(ItemStatus.ON_SALE, itemCaptor.getValue().getStatus());
        ArgumentCaptor<TradeOrder> orderCaptor = ArgumentCaptor.forClass(TradeOrder.class);
        verify(tradeOrderMapper).updateById(orderCaptor.capture());
        assertEquals(OrderStatus.CANCELLED, orderCaptor.getValue().getOrderStatus());
        assertEquals(PaymentStatus.CANCELLED, orderCaptor.getValue().getPaymentStatus());
        assertEquals(CancelledByType.ADMIN, orderCaptor.getValue().getCancelledBy());
        ArgumentCaptor<OrderStatusLog> logCaptor = ArgumentCaptor.forClass(OrderStatusLog.class);
        verify(orderStatusLogMapper).insert(logCaptor.capture());
        assertEquals("cancelled", logCaptor.getValue().getToStatus());
        verify(adminOperationLogMapper).insert(any(com.campus.secondhand.entity.AdminOperationLog.class));
    }

    @Test
    void shouldCloseCompletedOrder() {
        TradeOrder order = TradeOrder.builder()
                .orderId(1L)
                .orderNo("ORD1")
                .buyerUserId(11L)
                .sellerUserId(22L)
                .orderType(OrderType.ONLINE_COD)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PAID)
                .orderStatus(OrderStatus.COMPLETED)
                .deliveryType(DeliveryType.DORM_DELIVERY)
                .receiverName("Alice")
                .receiverPhone("13800000000")
                .deliveryAddress("Dorm 101")
                .totalAmount(new BigDecimal("1999.00"))
                .build();
        when(tradeOrderMapper.selectById(1L)).thenReturn(order);
        when(orderItemMapper.selectList(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<com.campus.secondhand.entity.OrderItem>>any())).thenReturn(List.of(OrderItem.builder()
                .orderItemId(9L)
                .orderId(1L)
                .itemId(101L)
                .itemTitleSnapshot("iPad Air")
                .itemPriceSnapshot(new BigDecimal("1999.00"))
                .quantity(1)
                .subtotalAmount(new BigDecimal("1999.00"))
                .build()));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                User.builder().userId(11L).studentNo("20240001").realName("Alice").phone("13800000000").build(),
                User.builder().userId(22L).studentNo("20240002").realName("Bob").phone("13900000000").build()
        ));
        when(orderStatusLogMapper.selectList(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<com.campus.secondhand.entity.OrderStatusLog>>any())).thenReturn(List.of());

        var response = adminOrderManagementService.close(
                new AdminPrincipal(9001L, "admin1001", "Campus Admin", "admin@campus.local", AdminRoleCode.SUPER_ADMIN),
                1L,
                new AdminOrderActionRequest("archive")
        );

        assertEquals("closed", response.orderStatus());
        ArgumentCaptor<TradeOrder> orderCaptor = ArgumentCaptor.forClass(TradeOrder.class);
        verify(tradeOrderMapper).updateById(orderCaptor.capture());
        assertEquals(OrderStatus.CLOSED, orderCaptor.getValue().getOrderStatus());
        ArgumentCaptor<OrderStatusLog> logCaptor = ArgumentCaptor.forClass(OrderStatusLog.class);
        verify(orderStatusLogMapper).insert(logCaptor.capture());
        assertEquals("closed", logCaptor.getValue().getToStatus());
        verify(adminOperationLogMapper).insert(any(com.campus.secondhand.entity.AdminOperationLog.class));
    }
}
