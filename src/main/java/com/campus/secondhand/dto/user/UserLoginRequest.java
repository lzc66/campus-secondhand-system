package com.campus.secondhand.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginRequest(
        @NotBlank(message = "studentNo is required")
        @Size(max = 64, message = "studentNo must be at most 64 characters")
        String studentNo,
        @NotBlank(message = "password is required") String password,
        @NotBlank(message = "captcha is required") String captcha,
        @NotBlank(message = "captchaKey is required") String captchaKey
) {
}