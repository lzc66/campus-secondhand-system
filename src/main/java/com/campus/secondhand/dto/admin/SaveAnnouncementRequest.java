package com.campus.secondhand.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record SaveAnnouncementRequest(
        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,
        @NotBlank(message = "content is required")
        String content,
        Boolean pinned,
        String publishStatus,
        LocalDateTime expireAt
) {
}
