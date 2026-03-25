package com.campus.secondhand.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminDashboardTrendPointResponse(
        LocalDate date,
        long createdOrderCount,
        long completedOrderCount,
        long cancelledOrderCount,
        BigDecimal completedAmount
) {
}