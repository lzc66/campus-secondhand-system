package com.campus.secondhand.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
        @NotBlank(message = "studentNo is required") String studentNo,
        @NotBlank(message = "password is required") String password,
        String captcha,
        String captchaKey
) {
}