package com.campus.secondhand.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserStatusRequest(
        @NotBlank(message = "accountStatus is required")
        String accountStatus,
        String actionNote
) {
}
