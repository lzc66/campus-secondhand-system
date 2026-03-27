package com.campus.secondhand.vo.admin;

import java.time.LocalDate;

public record AdminDashboardUserGrowthResponse(
        LocalDate date,
        long newUserCount,
        long cumulativeUserCount
) {
}