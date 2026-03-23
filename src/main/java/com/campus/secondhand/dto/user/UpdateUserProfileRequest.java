package com.campus.secondhand.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank(message = "realName is required")
        @Size(max = 50, message = "realName must be at most 50 characters")
        String realName,
        @NotBlank(message = "email is required")
        @Email(message = "email is invalid")
        @Size(max = 120, message = "email must be at most 120 characters")
        String email,
        @Size(max = 20, message = "phone must be at most 20 characters")
        String phone,
        @Size(max = 20, message = "qqNo must be at most 20 characters")
        String qqNo,
        @Size(max = 64, message = "wechatNo must be at most 64 characters")
        String wechatNo,
        @Size(max = 100, message = "collegeName must be at most 100 characters")
        String collegeName,
        @Size(max = 100, message = "majorName must be at most 100 characters")
        String majorName,
        @Size(max = 100, message = "className must be at most 100 characters")
        String className,
        @Size(max = 255, message = "dormitoryAddress must be at most 255 characters")
        String dormitoryAddress
) {
}