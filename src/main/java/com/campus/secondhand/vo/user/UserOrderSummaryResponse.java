package com.campus.secondhand.vo.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserOrderSummaryResponse(
        Long orderId,
        String orderNo,
        String role,
        String counterpartName,
        Long itemId,
        String itemTitle,
        Integer quantity,
        BigDecimal totalAmount,
        String orderStatus,
        String paymentStatus,
        String deliveryType,
        LocalDateTime createdAt
) {
}
