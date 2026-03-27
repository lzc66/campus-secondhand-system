package com.campus.secondhand.vo.publicapi;

public record PublicWantedRequesterResponse(
        Long userId,
        String realName,
        String avatarUrl,
        String collegeName,
        String majorName,
        String className
) {
}
