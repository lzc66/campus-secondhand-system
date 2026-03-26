package com.campus.secondhand.vo.user;

public record UserCaptchaResponse(
        String captchaKey,
        String imageData,
        long expiresInSeconds
) {
}