package com.campus.secondhand.dto.user;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record SaveItemRequest(
        @NotNull(message = "categoryId is required")
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
        @NotBlank(message = "conditionLevel is required")
        String conditionLevel,
        @NotNull(message = "price is required")
        @DecimalMin(value = "0.01", message = "price must be greater than 0")
        BigDecimal price,
        BigDecimal originalPrice,
        @NotNull(message = "stock is required")
        @Min(value = 1, message = "stock must be at least 1")
        Integer stock,
        @NotBlank(message = "tradeMode is required")
        String tradeMode,
        Boolean negotiable,
        @Size(max = 20, message = "contactPhone must be at most 20 characters")
        String contactPhone,
        @Size(max = 20, message = "contactQq must be at most 20 characters")
        String contactQq,
        @Size(max = 64, message = "contactWechat must be at most 64 characters")
        String contactWechat,
        @Size(max = 255, message = "pickupAddress must be at most 255 characters")
        String pickupAddress,
        String status,
        @NotEmpty(message = "imageFileIds is required")
        List<Long> imageFileIds,
        Long coverImageFileId
) {
}