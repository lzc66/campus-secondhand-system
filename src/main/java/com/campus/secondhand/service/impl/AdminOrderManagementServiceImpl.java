package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.admin.AdminOrderActionRequest;
import com.campus.secondhand.entity.AdminOperationLog;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.OrderItem;
import com.campus.secondhand.entity.OrderStatusLog;
import com.campus.secondhand.entity.TradeOrder;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.CancelledByType;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.enums.OrderOperatorType;
import com.campus.secondhand.enums.OrderStatus;
import com.campus.secondhand.enums.PaymentStatus;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.OrderItemMapper;
import com.campus.secondhand.mapper.OrderStatusLogMapper;
import com.campus.secondhand.mapper.TradeOrderMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.AdminOrderManagementService;
import com.campus.secondhand.vo.admin.AdminOrderDetailResponse;
import com.campus.secondhand.vo.admin.AdminOrderPageResponse;
import com.campus.secondhand.vo.admin.AdminOrderStatusLogResponse;
import com.campus.secondhand.vo.admin.AdminOrderSummaryResponse;
import com.campus.secondhand.vo.user.OrderItemResponse;
import com.campus.secondhand.vo.user.OrderPartyResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminOrderManagementServiceImpl implements AdminOrderManagementService {

    private final TradeOrderMapper tradeOrderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;

    public AdminOrderManagementServiceImpl(TradeOrderMapper tradeOrderMapper,
                                           OrderItemMapper orderItemMapper,
                                           OrderStatusLogMapper orderStatusLogMapper,
                                           ItemMapper itemMapper,
                                           UserMapper userMapper,
                                           AdminOperationLogMapper adminOperationLogMapper) {
        this.tradeOrderMapper = tradeOrderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderStatusLogMapper = orderStatusLogMapper;
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
    }

    @Override
    public AdminOrderPageResponse list(String orderStatus, String orderNo, String buyerStudentNo, String sellerStudentNo, long page, long size) {
        OrderStatus statusFilter = parseStatusFilter(orderStatus);
        Long buyerUserId = resolveUserIdByStudentNo(buyerStudentNo);
        Long sellerUserId = resolveUserIdByStudentNo(sellerStudentNo);
        if ((StringUtils.hasText(buyerStudentNo) && buyerUserId == null) || (StringUtils.hasText(sellerStudentNo) && sellerUserId == null)) {
            return new AdminOrderPageResponse(Math.max(page, 1), Math.max(size, 1), 0, List.of());
        }
        Page<TradeOrder> queryPage = new Page<>(Math.max(page, 1), Math.max(size, 1));
        Page<TradeOrder> result = tradeOrderMapper.selectPage(queryPage, new LambdaQueryWrapper<TradeOrder>()
                .eq(statusFilter != null, TradeOrder::getOrderStatus, statusFilter)
                .eq(buyerUserId != null, TradeOrder::getBuyerUserId, buyerUserId)
                .eq(sellerUserId != null, TradeOrder::getSellerUserId, sellerUserId)
                .like(StringUtils.hasText(orderNo), TradeOrder::getOrderNo, orderNo == null ? null : orderNo.trim())
                .orderByDesc(TradeOrder::getCreatedAt));
        List<Long> orderIds = result.getRecords().stream().map(TradeOrder::getOrderId).toList();
        Map<Long, OrderItem> firstItemMap = orderIds.isEmpty() ? Map.of() : orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds)
                .orderByAsc(OrderItem::getOrderItemId)).stream()
                .collect(Collectors.toMap(OrderItem::getOrderId, item -> item, (a, b) -> a, LinkedHashMap::new));
        Map<Long, User> userMap = loadUsers(result.getRecords().stream()
                .flatMap(order -> java.util.stream.Stream.of(order.getBuyerUserId(), order.getSellerUserId()))
                .distinct().toList());
        List<AdminOrderSummaryResponse> records = result.getRecords().stream()
                .map(order -> toSummary(order, firstItemMap.get(order.getOrderId()), userMap))
                .toList();
        return new AdminOrderPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public AdminOrderDetailResponse detail(Long orderId) {
        return buildDetailResponse(getRequiredOrder(orderId));
    }

    @Override
    @Transactional
    public AdminOrderDetailResponse cancel(AdminPrincipal principal, Long orderId, AdminOrderActionRequest request) {
        TradeOrder order = getRequiredOrder(orderId);
        if (order.getOrderStatus() != OrderStatus.PENDING_CONFIRM
                && order.getOrderStatus() != OrderStatus.AWAITING_DELIVERY
                && order.getOrderStatus() != OrderStatus.DELIVERING) {
            throw new BusinessException(40980, HttpStatus.CONFLICT, "Current order status cannot be cancelled by admin");
        }
        OrderStatus fromStatus = order.getOrderStatus();
        String actionNote = trimToNull(request == null ? null : request.actionNote());
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.CANCELLED);
        order.setCancelledBy(CancelledByType.ADMIN);
        order.setCancelReason(actionNote == null ? "Cancelled by admin" : actionNote);
        order.setCancelledAt(LocalDateTime.now());
        tradeOrderMapper.updateById(order);

        List<OrderItem> orderItems = getOrderItems(order.getOrderId());
        for (OrderItem orderItem : orderItems) {
            Item item = itemMapper.selectById(orderItem.getItemId());
            if (item == null) {
                continue;
            }
            restoreStock(item, orderItem.getQuantity());
        }
        insertStatusLog(order.getOrderId(), OrderOperatorType.ADMIN, principal.getAdminId(), fromStatus, OrderStatus.CANCELLED, actionNote);
        logOperation(principal.getAdminId(), order.getOrderId(), "cancel", "{\"orderStatus\":\"cancelled\"}");
        return buildDetailResponse(order);
    }

    @Override
    @Transactional
    public AdminOrderDetailResponse close(AdminPrincipal principal, Long orderId, AdminOrderActionRequest request) {
        TradeOrder order = getRequiredOrder(orderId);
        if (order.getOrderStatus() != OrderStatus.COMPLETED && order.getOrderStatus() != OrderStatus.CANCELLED) {
            throw new BusinessException(40981, HttpStatus.CONFLICT, "Only completed or cancelled orders can be closed");
        }
        OrderStatus fromStatus = order.getOrderStatus();
        String actionNote = trimToNull(request == null ? null : request.actionNote());
        order.setOrderStatus(OrderStatus.CLOSED);
        tradeOrderMapper.updateById(order);
        insertStatusLog(order.getOrderId(), OrderOperatorType.ADMIN, principal.getAdminId(), fromStatus, OrderStatus.CLOSED, actionNote);
        logOperation(principal.getAdminId(), order.getOrderId(), "close", "{\"orderStatus\":\"closed\"}");
        return buildDetailResponse(order);
    }

    private TradeOrder getRequiredOrder(Long orderId) {
        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(40480, HttpStatus.NOT_FOUND, "Order not found");
        }
        return order;
    }

    private List<OrderItem> getOrderItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getOrderItemId));
    }

    private void restoreStock(Item item, Integer quantity) {
        item.setStock((item.getStock() == null ? 0 : item.getStock()) + (quantity == null ? 0 : quantity));
        if (item.getStatus() != ItemStatus.DELETED) {
            item.setStatus(ItemStatus.ON_SALE);
            item.setSoldAt(null);
        }
        itemMapper.updateById(item);
    }

    private AdminOrderSummaryResponse toSummary(TradeOrder order, OrderItem orderItem, Map<Long, User> userMap) {
        User buyer = userMap.get(order.getBuyerUserId());
        User seller = userMap.get(order.getSellerUserId());
        return new AdminOrderSummaryResponse(
                order.getOrderId(),
                order.getOrderNo(),
                order.getBuyerUserId(),
                buyer == null ? null : buyer.getStudentNo(),
                buyer == null ? null : buyer.getRealName(),
                order.getSellerUserId(),
                seller == null ? null : seller.getStudentNo(),
                seller == null ? null : seller.getRealName(),
                orderItem == null ? null : orderItem.getItemId(),
                orderItem == null ? null : orderItem.getItemTitleSnapshot(),
                orderItem == null ? null : orderItem.getQuantity(),
                order.getTotalAmount(),
                order.getOrderStatus().getValue(),
                order.getPaymentStatus().getValue(),
                order.getCreatedAt()
        );
    }

    private AdminOrderDetailResponse buildDetailResponse(TradeOrder order) {
        List<OrderItem> orderItems = getOrderItems(order.getOrderId());
        Map<Long, User> userMap = loadUsers(List.of(order.getBuyerUserId(), order.getSellerUserId()));
        List<OrderStatusLog> statusLogs = orderStatusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, order.getOrderId())
                .orderByAsc(OrderStatusLog::getCreatedAt)
                .orderByAsc(OrderStatusLog::getOrderStatusLogId));
        User buyer = userMap.get(order.getBuyerUserId());
        User seller = userMap.get(order.getSellerUserId());
        return new AdminOrderDetailResponse(
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
                order.getCancelledBy() == null ? null : order.getCancelledBy().getValue(),
                order.getCancelReason(),
                order.getConfirmedAt(),
                order.getDeliveredAt(),
                order.getCompletedAt(),
                order.getCancelledAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                toParty(buyer),
                toParty(seller),
                orderItems.stream().map(this::toOrderItemResponse).toList(),
                statusLogs.stream().map(this::toStatusLogResponse).toList()
        );
    }

    private OrderPartyResponse toParty(User user) {
        if (user == null) {
            return null;
        }
        return new OrderPartyResponse(user.getUserId(), user.getStudentNo(), user.getRealName(), user.getPhone());
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getOrderItemId(),
                item.getItemId(),
                item.getItemTitleSnapshot(),
                item.getItemPriceSnapshot(),
                item.getQuantity(),
                item.getSubtotalAmount()
        );
    }

    private AdminOrderStatusLogResponse toStatusLogResponse(OrderStatusLog log) {
        return new AdminOrderStatusLogResponse(
                log.getOrderStatusLogId(),
                log.getOperatorType() == null ? null : log.getOperatorType().getValue(),
                log.getOperatorId(),
                log.getFromStatus(),
                log.getToStatus(),
                log.getActionNote(),
                log.getCreatedAt()
        );
    }

    private Map<Long, User> loadUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds.stream().filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getUserId, user -> user, (a, b) -> a, LinkedHashMap::new));
    }

    private Long resolveUserIdByStudentNo(String studentNo) {
        if (!StringUtils.hasText(studentNo)) {
            return null;
        }
        User user = userMapper.selectByStudentNo(studentNo.trim());
        return user == null ? null : user.getUserId();
    }

    private OrderStatus parseStatusFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "pending_confirm" -> OrderStatus.PENDING_CONFIRM;
            case "awaiting_delivery" -> OrderStatus.AWAITING_DELIVERY;
            case "delivering" -> OrderStatus.DELIVERING;
            case "completed" -> OrderStatus.COMPLETED;
            case "cancelled" -> OrderStatus.CANCELLED;
            case "closed" -> OrderStatus.CLOSED;
            default -> throw new BusinessException(40080, HttpStatus.BAD_REQUEST, "orderStatus filter is invalid");
        };
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

    private void logOperation(Long adminId, Long orderId, String operationType, String operationDetail) {
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminId(adminId)
                .targetType("order")
                .targetId(orderId)
                .operationType(operationType)
                .operationDetail(operationDetail)
                .ipAddress(null)
                .build());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
