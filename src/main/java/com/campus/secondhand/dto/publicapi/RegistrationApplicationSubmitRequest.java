package com.campus.secondhand.dto.publicapi;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrationApplicationSubmitRequest(
        @NotBlank(message = "studentNo is required")
        @Size(max = 20, message = "studentNo must be at most 20 characters")
        String studentNo,
        @NotBlank(message = "realName is required")
        @Size(max = 50, message = "realName must be at most 50 characters")
        String realName,
        @NotBlank(message = "gender is required")
        String gender,
        @NotBlank(message = "email is required")
        @Email(message = "email is invalid")
        @Size(max = 120, message = "email must be at most 120 characters")
        String email,
        @Size(max = 20, message = "phone must be at most 20 characters")
        String phone,
        @NotBlank(message = "password is required")
        @Size(min = 6, max = 64, message = "password length must be between 6 and 64")
        String password,
        @NotBlank(message = "collegeName is required")
        @Size(max = 100, message = "collegeName must be at most 100 characters")
        String collegeName,
        @Size(max = 100, message = "majorName must be at most 100 characters")
        String majorName,
        @Size(max = 100, message = "className must be at most 100 characters")
        String className,
        @NotNull(message = "studentCardFileId is required")
        Long studentCardFileId
) {
}