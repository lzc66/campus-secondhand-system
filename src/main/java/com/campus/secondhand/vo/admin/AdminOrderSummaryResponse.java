package com.campus.secondhand.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminOrderSummaryResponse(
        Long orderId,
        String orderNo,
        Long buyerUserId,
        String buyerStudentNo,
        String buyerName,
        Long sellerUserId,
        String sellerStudentNo,
        String sellerName,
        Long itemId,
        String itemTitle,
        Integer quantity,
        BigDecimal totalAmount,
        String orderStatus,
        String paymentStatus,
        LocalDateTime createdAt
) {
}
