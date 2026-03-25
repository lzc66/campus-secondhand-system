package com.campus.secondhand.vo.admin;

import java.math.BigDecimal;

public record AdminDashboardCategorySalesResponse(
        Long categoryId,
        String categoryName,
        long soldQuantity,
        long completedOrderCount,
        BigDecimal completedAmount
) {
}