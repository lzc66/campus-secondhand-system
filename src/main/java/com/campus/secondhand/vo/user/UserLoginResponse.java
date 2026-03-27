package com.campus.secondhand.vo.user;

public record UserLoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserProfileResponse userProfile
) {
}