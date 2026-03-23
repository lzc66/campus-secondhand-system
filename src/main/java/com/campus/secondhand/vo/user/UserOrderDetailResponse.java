package com.campus.secondhand.vo.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record UserOrderDetailResponse(
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
        String cancelReason,
        LocalDateTime confirmedAt,
        LocalDateTime deliveredAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        OrderPartyResponse buyer,
        OrderPartyResponse seller,
        List<OrderItemResponse> items
) {
}
