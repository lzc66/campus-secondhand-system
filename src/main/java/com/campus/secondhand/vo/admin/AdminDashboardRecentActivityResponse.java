package com.campus.secondhand.vo.admin;

import java.time.LocalDateTime;

public record AdminDashboardRecentActivityResponse(
        Long adminOperationLogId,
        Long adminId,
        String adminNo,
        String adminName,
        String targetType,
        Long targetId,
        String operationType,
        String operationDetail,
        LocalDateTime createdAt
) {
}