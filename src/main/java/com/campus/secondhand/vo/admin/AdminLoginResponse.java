package com.campus.secondhand.vo.admin;

public record AdminLoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        AdminProfileResponse adminProfile
) {
}
