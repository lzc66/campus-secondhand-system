package com.campus.secondhand.vo.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record UserItemDetailResponse(
        Long itemId,
        Long categoryId,
        String categoryName,
        String title,
        String brand,
        String model,
        String description,
        String conditionLevel,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer stock,
        String tradeMode,
        boolean negotiable,
        String contactPhone,
        String contactQq,
        String contactWechat,
        String pickupAddress,
        String status,
        Integer viewCount,
        Integer commentCount,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ItemImageResponse> images
) {
}