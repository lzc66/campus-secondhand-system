package com.campus.secondhand.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record UpdateItemStatusRequest(
        @NotBlank(message = "itemStatus is required")
        String itemStatus,
        String actionNote
) {
}
