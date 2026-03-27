package com.campus.secondhand.vo.admin;

import java.math.BigDecimal;

public record AdminDashboardOverviewResponse(
        long totalUsers,
        long activeUsers,
        long pendingRegistrationCount,
        long totalItems,
        long onSaleItemCount,
        long totalOrders,
        long completedOrderCount,
        long totalWantedPosts,
        long publishedAnnouncementCount,
        long todayNewUsers,
        long todayNewItems,
        long todayNewOrders,
        BigDecimal todayCompletedAmount
) {
}