package com.campus.secondhand.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        @NotBlank(message = "adminNo is required") String adminNo,
        @NotBlank(message = "password is required") String password,
        @NotBlank(message = "captcha is required") String captcha,
        @NotBlank(message = "captchaKey is required") String captchaKey
) {
}