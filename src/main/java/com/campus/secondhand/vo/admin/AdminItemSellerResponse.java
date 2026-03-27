package com.campus.secondhand.vo.admin;

public record AdminItemSellerResponse(
        Long userId,
        String studentNo,
        String realName,
        String email,
        String phone
) {
}
