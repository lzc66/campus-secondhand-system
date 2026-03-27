package com.campus.secondhand.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateItemCommentRequest(
        @NotBlank(message = "content is required")
        @Size(max = 1000, message = "content must be at most 1000 characters")
        String content
) {
}