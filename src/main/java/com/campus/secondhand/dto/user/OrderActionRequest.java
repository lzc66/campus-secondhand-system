package com.campus.secondhand.dto.user;

import jakarta.validation.constraints.Size;

public record OrderActionRequest(
        @Size(max = 255, message = "actionNote must be at most 255 characters")
        String actionNote
) {
}
