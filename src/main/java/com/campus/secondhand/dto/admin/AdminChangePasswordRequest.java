package com.campus.secondhand.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminChangePasswordRequest(
        @NotBlank(message = "currentPassword is required")
        String currentPassword,
        @NotBlank(message = "newPassword is required")
        @Size(min = 6, max = 64, message = "newPassword length must be between 6 and 64")
        String newPassword
) {
}
