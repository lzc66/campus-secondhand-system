package com.campus.secondhand.dto.admin;

import jakarta.validation.constraints.Size;

public record AnnouncementActionRequest(
        @Size(max = 255, message = "actionNote must be at most 255 characters")
        String actionNote
) {
}
