package com.campus.secondhand.vo.admin;

public record AdminProfileResponse(
        Long adminId,
        String adminNo,
        String adminName,
        String email,
        String roleCode,
        String accountStatus
) {
}
