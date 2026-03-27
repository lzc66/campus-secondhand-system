package com.campus.secondhand.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @NotBlank(message = "cancelReason is required")
        @Size(max = 255, message = "cancelReason must be at most 255 characters")
        String cancelReason
) {
}
