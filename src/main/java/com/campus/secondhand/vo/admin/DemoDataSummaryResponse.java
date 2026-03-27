package com.campus.secondhand.vo.admin;

public record DemoDataSummaryResponse(
        long users,
        long items,
        long orders,
        long wantedPosts,
        long announcements,
        long pendingRegistrations
) {
}