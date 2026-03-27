package com.campus.secondhand.vo.publicapi;

import com.campus.secondhand.vo.user.ItemImageResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PublicItemDetailResponse(
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
        Integer viewCount,
        Integer commentCount,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        PublicSellerResponse seller,
        List<ItemImageResponse> images
) {
}