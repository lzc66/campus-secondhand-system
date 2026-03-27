package com.campus.secondhand.vo.admin;

import java.time.LocalDateTime;

public record AdminUserSummaryResponse(
        Long userId,
        String studentNo,
        String realName,
        String email,
        String phone,
        String collegeName,
        String accountStatus,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
