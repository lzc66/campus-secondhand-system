package com.campus.secondhand.dto.user;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotNull(message = "itemId is required")
        Long itemId,
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity,
        @NotBlank(message = "receiverName is required")
        @Size(max = 50, message = "receiverName must be at most 50 characters")
        String receiverName,
        @NotBlank(message = "receiverPhone is required")
        @Size(max = 20, message = "receiverPhone must be at most 20 characters")
        String receiverPhone,
        @NotBlank(message = "deliveryType is required")
        String deliveryType,
        @NotBlank(message = "deliveryAddress is required")
        @Size(max = 255, message = "deliveryAddress must be at most 255 characters")
        String deliveryAddress,
        @Size(max = 255, message = "buyerRemark must be at most 255 characters")
        String buyerRemark
) {
}
