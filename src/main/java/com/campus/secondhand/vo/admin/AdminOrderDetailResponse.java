package com.campus.secondhand.vo.admin;

import com.campus.secondhand.vo.user.OrderItemResponse;
import com.campus.secondhand.vo.user.OrderPartyResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderDetailResponse(
        Long orderId,
        String orderNo,
        String orderType,
        String paymentMethod,
        String paymentStatus,
        String orderStatus,
        String deliveryType,
        String receiverName,
        String receiverPhone,
        String deliveryAddress,
        BigDecimal totalAmount,
        String buyerRemark,
        String sellerRemark,
        String cancelledBy,
        String cancelReason,
        LocalDateTime confirmedAt,
        LocalDateTime deliveredAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        OrderPartyResponse buyer,
        OrderPartyResponse seller,
        List<OrderItemResponse> items,
        List<AdminOrderStatusLogResponse> statusLogs
) {
}
