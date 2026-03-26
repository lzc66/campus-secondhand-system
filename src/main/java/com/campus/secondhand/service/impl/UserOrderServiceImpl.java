package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.user.CancelOrderRequest;
import com.campus.secondhand.dto.user.CreateOrderRequest;
import com.campus.secondhand.dto.user.OrderActionRequest;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.OrderItem;
import com.campus.secondhand.entity.OrderStatusLog;
import com.campus.secondhand.entity.TradeOrder;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.CancelledByType;
import com.campus.secondhand.enums.DeliveryType;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.enums.OrderOperatorType;
import com.campus.secondhand.enums.OrderStatus;
import com.campus.secondhand.enums.OrderType;
import com.campus.secondhand.enums.PaymentMethod;
import com.campus.secondhand.enums.PaymentStatus;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.OrderItemMapper;
import com.campus.secondhand.mapper.OrderStatusLogMapper;
import com.campus.secondhand.mapper.TradeOrderMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.RecommendationBehaviorService;
import com.campus.secondhand.service.UserOrderService;
import com.campus.secondhand.vo.user.OrderItemResponse;
import com.campus.secondhand.vo.user.OrderPartyResponse;
import com.campus.secondhand.vo.user.UserOrderDetailResponse;
import com.campus.secondhand.vo.user.UserOrderPageResponse;
import com.campus.secondhand.vo.user.UserOrderSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class UserOrderServiceImpl implements UserOrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TradeOrderMapper tradeOrderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final RecommendationBehaviorService recommendationBehaviorService;

    public UserOrderServiceImpl(TradeOrderMapper tradeOrderMapper,
                                OrderItemMapper orderItemMapper,
                                OrderStatusLogMapper orderStatusLogMapper,
                                ItemMapper itemMapper,
                                UserMapper userMapper,
                                RecommendationBehaviorService recommendationBehaviorService) {
        this.tradeOrderMapper = tradeOrderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderStatusLogMapper = orderStatusLogMapper;
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
        this.recommendationBehaviorService = recommendationBehaviorService;
    }

    @Override
    @Transactional
    public UserOrderDetailResponse createOrder(UserPrincipal principal, CreateOrderRequest request) {
        Item item = getPurchasableItem(request.itemId());
        if (Objects.equals(item.getSellerUserId(), principal.getUserId())) {
            throw new BusinessException(40980, HttpStatus.CONFLICT, "You cannot buy your own item");
        }
        if (request.quantity() > item.getStock()) {
            throw new BusinessException(40981, HttpStatus.CONFLICT, "Insufficient stock");
        }
        User buyer = getRequiredUser(principal.getUserId());
        User seller = getRequiredUser(item.getSellerUserId());
        DeliveryType deliveryType = parseDeliveryType(request.deliveryType());
        BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(request.quantity()));
        TradeOrder order = TradeOrder.builder()
                .orderNo(generateOrderNo())
                .buyerUserId(buyer.getUserId())
                .sellerUserId(seller.getUserId())
                .orderType(OrderType.ONLINE_COD)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.UNPAID)
                .orderStatus(OrderStatus.PENDING_CONFIRM)
                .deliveryType(deliveryType)
                .receiverName(request.receiverName().trim())
                .receiverPhone(request.receiverPhone().trim())
                .deliveryAddress(request.deliveryAddress().trim())
                .totalAmount(subtotal)
                .buyerRemark(trimToNull(request.buyerRemark()))
                .build();
        tradeOrderMapper.insert(order);
        orderItemMapper.insert(OrderItem.builder()
                .orderId(order.getOrderId())
                .itemId(item.getItemId())
                .itemTitleSnapshot(item.getTitle())
                .itemPriceSnapshot(item.getPrice())
                .quantity(request.quantity())
                .subtotalAmount(subtotal)
                .build());
        reduceItemStock(item, request.quantity());
        insertStatusLog(order.getOrderId(), OrderOperatorType.BUYER, buyer.getUserId(), null, OrderStatus.PENDING_CONFIRM, "Buyer submitted order");
        return buildDetailResponse(order);
    }

    @Override
    public UserOrderPageResponse listOrders(UserPrincipal principal, String role, String status, long page, long size) {
        String normalizedRole = normalizeRole(role);
        OrderStatus statusFilter = parseStatusFilter(status);
        Page<TradeOrder> queryPage = new Page<>(Math.max(page, 1), Math.max(size, 1));
        LambdaQueryWrapper<TradeOrder> wrapper = new LambdaQueryWrapper<TradeOrder>()
                .eq("buyer".equals(normalizedRole), TradeOrder::getBuyerUserId, principal.getUserId())
                .eq("seller".equals(normalizedRole), TradeOrder::getSellerUserId, principal.getUserId())
                .eq(statusFilter != null, TradeOrder::getOrderStatus, statusFilter)
                .orderByDesc(TradeOrder::getCreatedAt);
        Page<TradeOrder> result = tradeOrderMapper.selectPage(queryPage, wrapper);
        List<Long> orderIds = result.getRecords().stream().map(TradeOrder::getOrderId).toList();
        Map<Long, OrderItem> firstItemMap = orderIds.isEmpty() ? Map.of() : orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds)
                .orderByAsc(OrderItem::getOrderItemId)).stream()
                .collect(Collectors.toMap(OrderItem::getOrderId, item -> item, (a, b) -> a));
        Map<Long, User> userMap = loadUsers(result.getRecords().stream()
                .flatMap(order -> java.util.stream.Stream.of(order.getBuyerUserId(), order.getSellerUserId()))
                .distinct().toList());
        List<UserOrderSummaryResponse> records = result.getRecords().stream()
                .map(order -> {
                    OrderItem orderItem = firstItemMap.get(order.getOrderId());
                    Long counterpartId = "buyer".equals(normalizedRole) ? order.getSellerUserId() : order.getBuyerUserId();
                    User counterpart = userMap.get(counterpartId);
                    return new UserOrderSummaryResponse(
                            order.getOrderId(),
                            order.getOrderNo(),
                            normalizedRole,
                            counterpart == null ? null : counterpart.getRealName(),
                            orderItem == null ? null : orderItem.getItemId(),
                            orderItem == null ? null : orderItem.getItemTitleSnapshot(),
                            orderItem == null ? null : orderItem.getQuantity(),
                            order.getTotalAmount(),
                            order.getOrderStatus() == null ? null : order.getOrderStatus().getValue(),
                            order.getPaymentStatus() == null ? null : order.getPaymentStatus().getValue(),
                            order.getDeliveryType() == null ? null : order.getDeliveryType().getValue(),
                            order.getCreatedAt()
                    );
                })
                .toList();
        return new UserOrderPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public UserOrderDetailResponse getOrderDetail(UserPrincipal principal, Long orderId) {
        return buildDetailResponse(getAccessibleOrder(principal.getUserId(), orderId));
    }

    @Override
    @Transactional
    public UserOrderDetailResponse confirmOrder(UserPrincipal principal, Long orderId, OrderActionRequest request) {
        TradeOrder order = getAccessibleOrder(principal.getUserId(), orderId);
        ensureSeller(order, principal.getUserId());
        ensureStatus(order, OrderStatus.PENDING_CONFIRM, "Only pending orders can be confirmed");
        order.setOrderStatus(OrderStatus.AWAITING_DELIVERY);
        order.setConfirmedAt(LocalDateTime.now());
        order.setSellerRemark(trimToNull(request == null ? null : request.actionNote()));
        tradeOrderMapper.updateById(order);
        insertStatusLog(order.getOrderId(), OrderOperatorType.SELLER, principal.getUserId(), OrderStatus.PENDING_CONFIRM, OrderStatus.AWAITING_DELIVERY, request == null ? null : trimToNull(request.actionNote()));
        return buildDetailResponse(order);
    }

    @Override
    @Transactional
    public UserOrderDetailResponse markDelivering(UserPrincipal principal, Long orderId, OrderActionRequest request) {
        TradeOrder order = getAccessibleOrder(principal.getUserId(), orderId);
        ensureSeller(order, principal.getUserId());
        ensureStatus(order, OrderStatus.AWAITING_DELIVERY, "Only confirmed orders can be marked as delivering");
        order.setOrderStatus(OrderStatus.DELIVERING);
        order.setDeliveredAt(LocalDateTime.now());
        order.setSellerRemark(trimToNull(request == null ? null : request.actionNote()));
        tradeOrderMapper.updateById(order);
        insertStatusLog(order.getOrderId(), OrderOperatorType.SELLER, principal.getUserId(), OrderStatus.AWAITING_DELIVERY, OrderStatus.DELIVERING, request == null ? null : trimToNull(request.actionNote()));
        return buildDetailResponse(order);
    }

    @Override
    @Transactional
    public UserOrderDetailResponse completeOrder(UserPrincipal principal, Long orderId, OrderActionRequest request) {
        TradeOrder order = getAccessibleOrder(principal.getUserId(), orderId);
        ensureBuyer(order, principal.getUserId());
        ensureStatus(order, OrderStatus.DELIVERING, "Only delivering orders can be completed");
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setCompletedAt(LocalDateTime.now());
        tradeOrderMapper.updateById(order);
        OrderItem orderItem = getRequiredOrderItem(order.getOrderId());
        Item item = getRequiredItem(orderItem.getItemId());
        if (item.getStock() != null && item.getStock() == 0) {
            item.setStatus(ItemStatus.SOLD);
            item.setSoldAt(LocalDateTime.now());
        } else {
            item.setStatus(ItemStatus.ON_SALE);
        }
        itemMapper.updateById(item);
        insertStatusLog(order.getOrderId(), OrderOperatorType.BUYER, principal.getUserId(), OrderStatus.DELIVERING, OrderStatus.COMPLETED, request == null ? null : trimToNull(request.actionNote()));
        recommendationBehaviorService.recordPurchase(principal.getUserId(), orderItem.getItemId(), order.getOrderId());
        return buildDetailResponse(order);
    }

    @Override
    @Transactional
    public UserOrderDetailResponse cancelOrder(UserPrincipal principal, Long orderId, CancelOrderRequest request) {
        TradeOrder order = getAccessibleOrder(principal.getUserId(), orderId);
        if (order.getOrderStatus() != OrderStatus.PENDING_CONFIRM && order.getOrderStatus() != OrderStatus.AWAITING_DELIVERY) {
            throw new BusinessException(40982, HttpStatus.CONFLICT, "Current order status cannot be cancelled");
        }
        boolean isBuyer = Objects.equals(order.getBuyerUserId(), principal.getUserId());
        boolean isSeller = Objects.equals(order.getSellerUserId(), principal.getUserId());
        if (!isBuyer && !isSeller) {
            throw new BusinessException(40380, HttpStatus.FORBIDDEN, "You cannot operate this order");
        }
        OrderStatus fromStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.CANCELLED);
        order.setCancelledBy(isBuyer ? CancelledByType.BUYER : CancelledByType.SELLER);
        order.setCancelReason(request.cancelReason().trim());
        order.setCancelledAt(LocalDateTime.now());
        tradeOrderMapper.updateById(order);
        OrderItem orderItem = getRequiredOrderItem(order.getOrderId());
        Item item = getRequiredItem(orderItem.getItemId());
        restoreItemStock(item, orderItem.getQuantity());
        insertStatusLog(order.getOrderId(), isBuyer ? OrderOperatorType.BUYER : OrderOperatorType.SELLER, principal.getUserId(), fromStatus, OrderStatus.CANCELLED, request.cancelReason().trim());
        return buildDetailResponse(order);
    }

    private UserOrderDetailResponse buildDetailResponse(TradeOrder order) {
        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getOrderId())
                .orderByAsc(OrderItem::getOrderItemId));
        Map<Long, User> users = loadUsers(List.of(order.getBuyerUserId(), order.getSellerUserId()));
        User buyer = users.get(order.getBuyerUserId());
        User seller = users.get(order.getSellerUserId());
        return new UserOrderDetailResponse(
                order.getOrderId(),
                order.getOrderNo(),
                order.getOrderType() == null ? null : order.getOrderType().getValue(),
                order.getPaymentMethod() == null ? null : order.getPaymentMethod().getValue(),
                order.getPaymentStatus() == null ? null : order.getPaymentStatus().getValue(),
                order.getOrderStatus() == null ? null : order.getOrderStatus().getValue(),
                order.getDeliveryType() == null ? null : order.getDeliveryType().getValue(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getDeliveryAddress(),
                order.getTotalAmount(),
                order.getBuyerRemark(),
                order.getSellerRemark(),
                order.getCancelReason(),
                order.getConfirmedAt(),
                order.getDeliveredAt(),
                order.getCompletedAt(),
                order.getCancelledAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                toParty(buyer),
                toParty(seller),
                orderItems.stream().map(this::toOrderItemResponse).toList()
        );
    }

    private OrderPartyResponse toParty(User user) {
        if (user == null) {
            return null;
        }
        return new OrderPartyResponse(user.getUserId(), user.getStudentNo(), user.getRealName(), user.getPhone());
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        return new OrderItemResponse(item.getOrderItemId(), item.getItemId(), item.getItemTitleSnapshot(), item.getItemPriceSnapshot(), item.getQuantity(), item.getSubtotalAmount());
    }

    private TradeOrder getAccessibleOrder(Long userId, Long orderId) {
        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null || (!Objects.equals(order.getBuyerUserId(), userId) && !Objects.equals(order.getSellerUserId(), userId))) {
            throw new BusinessException(40480, HttpStatus.NOT_FOUND, "Order not found");
        }
        return order;
    }

    private Item getPurchasableItem(Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null || item.getStatus() != ItemStatus.ON_SALE) {
            throw new BusinessException(40450, HttpStatus.NOT_FOUND, "Item not found");
        }
        if (item.getStock() == null || item.getStock() <= 0) {
            throw new BusinessException(40983, HttpStatus.CONFLICT, "Item is out of stock");
        }
        return item;
    }

    private Item getRequiredItem(Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(40450, HttpStatus.NOT_FOUND, "Item not found");
        }
        return item;
    }

    private OrderItem getRequiredOrderItem(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        if (items.isEmpty()) {
            throw new BusinessException(40481, HttpStatus.NOT_FOUND, "Order item not found");
        }
        return items.get(0);
    }

    private User getRequiredUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40420, HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }

    private Map<Long, User> loadUsers(List<Long> userIds) {
        List<Long> normalizedUserIds = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (normalizedUserIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(normalizedUserIds)
                .stream().collect(Collectors.toMap(User::getUserId, user -> user));
    }

    private void ensureSeller(TradeOrder order, Long userId) {
        if (!Objects.equals(order.getSellerUserId(), userId)) {
            throw new BusinessException(40381, HttpStatus.FORBIDDEN, "Only the seller can operate this order");
        }
    }

    private void ensureBuyer(TradeOrder order, Long userId) {
        if (!Objects.equals(order.getBuyerUserId(), userId)) {
            throw new BusinessException(40382, HttpStatus.FORBIDDEN, "Only the buyer can operate this order");
        }
    }

    private void ensureStatus(TradeOrder order, OrderStatus expected, String message) {
        if (order.getOrderStatus() != expected) {
            throw new BusinessException(40984, HttpStatus.CONFLICT, message);
        }
    }

    private DeliveryType parseDeliveryType(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "dorm_delivery" -> DeliveryType.DORM_DELIVERY;
            case "self_pickup" -> DeliveryType.SELF_PICKUP;
            case "face_to_face" -> DeliveryType.FACE_TO_FACE;
            default -> throw new BusinessException(40080, HttpStatus.BAD_REQUEST, "deliveryType is invalid");
        };
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "buyer";
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        if (!"buyer".equals(normalized) && !"seller".equals(normalized)) {
            throw new BusinessException(40081, HttpStatus.BAD_REQUEST, "role must be buyer or seller");
        }
        return normalized;
    }

    private OrderStatus parseStatusFilter(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "pending_confirm" -> OrderStatus.PENDING_CONFIRM;
            case "awaiting_delivery" -> OrderStatus.AWAITING_DELIVERY;
            case "delivering" -> OrderStatus.DELIVERING;
            case "completed" -> OrderStatus.COMPLETED;
            case "cancelled" -> OrderStatus.CANCELLED;
            case "closed" -> OrderStatus.CLOSED;
            default -> throw new BusinessException(40082, HttpStatus.BAD_REQUEST, "status filter is invalid");
        };
    }

    private void reduceItemStock(Item item, int quantity) {
        int remaining = item.getStock() - quantity;
        item.setStock(remaining);
        item.setStatus(remaining == 0 ? ItemStatus.RESERVED : ItemStatus.ON_SALE);
        itemMapper.updateById(item);
    }

    private void restoreItemStock(Item item, int quantity) {
        item.setStock((item.getStock() == null ? 0 : item.getStock()) + quantity);
        item.setStatus(ItemStatus.ON_SALE);
        item.setSoldAt(null);
        itemMapper.updateById(item);
    }

    private void insertStatusLog(Long orderId,
                                 OrderOperatorType operatorType,
                                 Long operatorId,
                                 OrderStatus fromStatus,
                                 OrderStatus toStatus,
                                 String note) {
        orderStatusLogMapper.insert(OrderStatusLog.builder()
                .orderId(orderId)
                .operatorType(operatorType)
                .operatorId(operatorId)
                .fromStatus(fromStatus == null ? null : fromStatus.getValue())
                .toStatus(toStatus.getValue())
                .actionNote(trimToNull(note))
                .build());
    }

    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(ORDER_NO_FORMATTER) + ThreadLocalRandom.current().nextInt(100000, 1000000);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
