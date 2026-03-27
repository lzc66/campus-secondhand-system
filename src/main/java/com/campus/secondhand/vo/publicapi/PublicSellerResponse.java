package com.campus.secondhand.vo.publicapi;

public record PublicSellerResponse(
        Long userId,
        String realName,
        String avatarUrl,
        String collegeName,
        String majorName,
        String className
) {
}