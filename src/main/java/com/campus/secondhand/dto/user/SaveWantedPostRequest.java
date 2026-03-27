package com.campus.secondhand.dto.user;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaveWantedPostRequest(
        Long categoryId,
        @NotBlank(message = "title is required")
        @Size(max = 150, message = "title must be at most 150 characters")
        String title,
        @Size(max = 80, message = "brand must be at most 80 characters")
        String brand,
        @Size(max = 80, message = "model must be at most 80 characters")
        String model,
        @NotBlank(message = "description is required")
        String description,
        @DecimalMin(value = "0.0", inclusive = true, message = "expectedPriceMin must be greater than or equal to 0")
        BigDecimal expectedPriceMin,
        @DecimalMin(value = "0.0", inclusive = true, message = "expectedPriceMax must be greater than or equal to 0")
        BigDecimal expectedPriceMax,
        @Size(max = 20, message = "contactPhone must be at most 20 characters")
        String contactPhone,
        @Size(max = 20, message = "contactQq must be at most 20 characters")
        String contactQq,
        @Size(max = 64, message = "contactWechat must be at most 64 characters")
        String contactWechat,
        String status,
        LocalDateTime expiresAt
) {
}
