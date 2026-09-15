package com.campus.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.user.CancelOrderRequest;
import com.campus.secondhand.dto.user.CreateOrderRequest;
import com.campus.secondhand.dto.user.OrderActionRequest;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.OrderItem;
import com.campus.secondhand.entity.TradeOrder;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.DeliveryType;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.enums.OrderStatus;
import com.campus.secondhand.enums.OrderType;
import com.campus.secondhand.enums.PaymentMethod;
import com.campus.secondhand.enums.PaymentStatus;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.OrderItemMapper;
import com.campus.secondhand.mapper.OrderStatusLogMapper;
import com.campus.secondhand.mapper.TradeOrderMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.impl.UserOrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserOrderServiceTest {

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
    private RecommendationBehaviorService recommendationBehaviorService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserOrderServiceImpl userOrderService;


    @Test
    void shouldReturnEmptyOrderPageWithoutQueryingUsersWhenNoOrdersFound() {
        Page<TradeOrder> emptyPage = new Page<>(1, 10, 0);
        emptyPage.setRecords(List.of());
        when(tradeOrderMapper.selectPage(any(), any())).thenReturn(emptyPage);

        var response = userOrderService.listOrders(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", UserAccountStatus.ACTIVE),
                "buyer",
                null,
                1,
                10
        );

        assertEquals(0, response.total());
        assertEquals(0, response.records().size());
    }

    @Test
    void shouldCreateOrderAndReserveStock() {
        when(itemMapper.selectById(101L)).thenReturn(Item.builder()
                .itemId(101L)
                .sellerUserId(22L)
                .title("iPad Air")
                .price(new BigDecimal("1999.00"))
                .stock(1)
                .status(ItemStatus.ON_SALE)
                .build());
        when(userMapper.selectById(11L)).thenReturn(User.builder().userId(11L).realName("Alice").studentNo("20240001").phone("13800000000").build());
        when(userMapper.selectById(22L)).thenReturn(User.builder().userId(22L).realName("Bob").studentNo("20240002").phone("13900000000").build());
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                User.builder().userId(11L).realName("Alice").studentNo("20240001").phone("13800000000").build(),
                User.builder().userId(22L).realName("Bob").studentNo("20240002").phone("13900000000").build()
        ));
        doAnswer(invocation -> {
            TradeOrder order = invocation.getArgument(0);
            order.setOrderId(1L);
            return 1;
        }).when(tradeOrderMapper).insert(any(TradeOrder.class));
        doAnswer(invocation -> {
            OrderItem orderItem = invocation.getArgument(0);
            orderItem.setOrderItemId(9L);
            return 1;
        }).when(orderItemMapper).insert(any(OrderItem.class));
        when(orderItemMapper.selectList(any())).thenReturn(List.of(OrderItem.builder()
                .orderItemId(9L)
                .orderId(1L)
                .itemId(101L)
                .itemTitleSnapshot("iPad Air")
                .itemPriceSnapshot(new BigDecimal("1999.00"))
                .quantity(1)
                .subtotalAmount(new BigDecimal("1999.00"))
                .build()));
        when(itemMapper.update(any(), any())).thenReturn(1);

        var response = userOrderService.createOrder(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", UserAccountStatus.ACTIVE),
                new CreateOrderRequest(101L, 1, "Alice", "13800000000", "dorm_delivery", "Dorm 101", "please call first")
        );

        assertEquals(OrderStatus.PENDING_CONFIRM.getValue(), response.orderStatus());
        // 库存通过条件原子扣减(而非整行回写)完成
        ArgumentCaptor<Wrapper<Item>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(itemMapper).update(isNull(), wrapperCaptor.capture());
        assertEquals("stock = stock - 1", wrapperCaptor.getValue().getSqlSet());
        // 库存扣减到 0 后商品被置为预留(captor 会同时匹配到 null 实体的扣减调用,过滤掉)
        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper, times(2)).update(itemCaptor.capture(), any(Wrapper.class));
        List<Item> updatedEntities = itemCaptor.getAllValues().stream().filter(Objects::nonNull).toList();
        assertEquals(1, updatedEntities.size());
        assertEquals(ItemStatus.RESERVED, updatedEntities.get(0).getStatus());
    }

    @Test
    void shouldRejectBuyingOwnItem() {
        when(itemMapper.selectById(101L)).thenReturn(Item.builder()
                .itemId(101L)
                .sellerUserId(11L)
                .price(new BigDecimal("1999.00"))
                .stock(1)
                .status(ItemStatus.ON_SALE)
                .build());

        assertThrows(BusinessException.class, () -> userOrderService.createOrder(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", UserAccountStatus.ACTIVE),
                new CreateOrderRequest(101L, 1, "Alice", "13800000000", "dorm_delivery", "Dorm 101", null)
        ));
    }

    @Test
    void shouldCompleteOrderAndMarkItemSold() {
        when(tradeOrderMapper.selectById(1L)).thenReturn(TradeOrder.builder()
                .orderId(1L)
                .orderNo("ORD1")
                .buyerUserId(11L)
                .sellerUserId(22L)
                .orderStatus(OrderStatus.DELIVERING)
                .paymentStatus(PaymentStatus.UNPAID)
                .orderType(OrderType.ONLINE_COD)
                .paymentMethod(PaymentMethod.COD)
                .deliveryType(DeliveryType.DORM_DELIVERY)
                .receiverName("Alice")
                .receiverPhone("13800000000")
                .deliveryAddress("Dorm 101")
                .totalAmount(new BigDecimal("1999.00"))
                .build());
        when(orderItemMapper.selectList(any())).thenReturn(List.of(OrderItem.builder()
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
                User.builder().userId(11L).realName("Alice").build(),
                User.builder().userId(22L).realName("Bob").build()
        ));
        when(itemMapper.update(any(), any())).thenReturn(1);

        var response = userOrderService.completeOrder(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", UserAccountStatus.ACTIVE),
                1L,
                new OrderActionRequest("received")
        );

        assertEquals(OrderStatus.COMPLETED.getValue(), response.orderStatus());
        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).update(itemCaptor.capture(), any(Wrapper.class));
        assertEquals(ItemStatus.SOLD, itemCaptor.getValue().getStatus());
        verify(recommendationBehaviorService).recordPurchase(11L, 101L, 1L);
    }

    @Test
    void shouldCancelOrderAndRestoreStock() {
        when(tradeOrderMapper.selectById(1L)).thenReturn(TradeOrder.builder()
                .orderId(1L)
                .orderNo("ORD1")
                .buyerUserId(11L)
                .sellerUserId(22L)
                .orderStatus(OrderStatus.PENDING_CONFIRM)
                .paymentStatus(PaymentStatus.UNPAID)
                .orderType(OrderType.ONLINE_COD)
                .paymentMethod(PaymentMethod.COD)
                .deliveryType(DeliveryType.DORM_DELIVERY)
                .receiverName("Alice")
                .receiverPhone("13800000000")
                .deliveryAddress("Dorm 101")
                .totalAmount(new BigDecimal("1999.00"))
                .build());
        when(orderItemMapper.selectList(any())).thenReturn(List.of(OrderItem.builder()
                .orderItemId(9L)
                .orderId(1L)
                .itemId(101L)
                .quantity(1)
                .itemTitleSnapshot("iPad Air")
                .itemPriceSnapshot(new BigDecimal("1999.00"))
                .subtotalAmount(new BigDecimal("1999.00"))
                .build()));
        when(itemMapper.selectById(101L)).thenReturn(Item.builder().itemId(101L).stock(0).status(ItemStatus.RESERVED).build());
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                User.builder().userId(11L).realName("Alice").build(),
                User.builder().userId(22L).realName("Bob").build()
        ));
        when(itemMapper.update(any(), any())).thenReturn(1);

        var response = userOrderService.cancelOrder(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", UserAccountStatus.ACTIVE),
                1L,
                new CancelOrderRequest("buyer changed mind")
        );

        assertEquals(OrderStatus.CANCELLED.getValue(), response.orderStatus());
        // 库存通过原子自增恢复
        ArgumentCaptor<Wrapper<Item>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(itemMapper).update(isNull(), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSet().contains("stock = stock + 1"));
        // 预留商品回到在售(captor 会同时匹配到 null 实体的自增调用,过滤掉)
        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper, times(2)).update(itemCaptor.capture(), any(Wrapper.class));
        List<Item> updatedEntities = itemCaptor.getAllValues().stream().filter(Objects::nonNull).toList();
        assertEquals(1, updatedEntities.size());
        assertEquals(ItemStatus.ON_SALE, updatedEntities.get(0).getStatus());
    }

    @Test
    void shouldNotResurrectDeletedItemOnCancel() {
        when(tradeOrderMapper.selectById(1L)).thenReturn(TradeOrder.builder()
                .orderId(1L)
                .orderNo("ORD1")
                .buyerUserId(11L)
                .sellerUserId(22L)
                .orderStatus(OrderStatus.PENDING_CONFIRM)
                .paymentStatus(PaymentStatus.UNPAID)
                .orderType(OrderType.ONLINE_COD)
                .paymentMethod(PaymentMethod.COD)
                .deliveryType(DeliveryType.DORM_DELIVERY)
                .receiverName("Alice")
                .receiverPhone("13800000000")
                .deliveryAddress("Dorm 101")
                .totalAmount(new BigDecimal("1999.00"))
                .build());
        when(orderItemMapper.selectList(any())).thenReturn(List.of(OrderItem.builder()
                .orderItemId(9L)
                .orderId(1L)
                .itemId(101L)
                .quantity(1)
                .itemTitleSnapshot("iPad Air")
                .itemPriceSnapshot(new BigDecimal("1999.00"))
                .subtotalAmount(new BigDecimal("1999.00"))
                .build()));
        // 商品已被卖家软删除:取消订单不得恢复库存,更不得把状态改回在售
        when(itemMapper.selectById(101L)).thenReturn(Item.builder()
                .itemId(101L)
                .stock(0)
                .status(ItemStatus.DELETED)
                .deletedAt(java.time.LocalDateTime.now())
                .build());
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                User.builder().userId(11L).realName("Alice").build(),
                User.builder().userId(22L).realName("Bob").build()
        ));

        var response = userOrderService.cancelOrder(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", UserAccountStatus.ACTIVE),
                1L,
                new CancelOrderRequest("buyer changed mind")
        );

        assertEquals(OrderStatus.CANCELLED.getValue(), response.orderStatus());
        verify(itemMapper, org.mockito.Mockito.never()).update(any(), any());
    }
}
