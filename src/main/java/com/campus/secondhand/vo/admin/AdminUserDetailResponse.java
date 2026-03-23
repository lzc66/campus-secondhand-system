package com.campus.secondhand.vo.admin;

import java.time.LocalDateTime;

public record AdminUserDetailResponse(
        Long userId,
        String studentNo,
        String realName,
        String email,
        String gender,
        String phone,
        String qqNo,
        String wechatNo,
        String avatarUrl,
        String collegeName,
        String majorName,
        String className,
        String dormitoryAddress,
        String accountStatus,
        Integer publishedItemCount,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
