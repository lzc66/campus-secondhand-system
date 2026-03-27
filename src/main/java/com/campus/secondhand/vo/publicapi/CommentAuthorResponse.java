package com.campus.secondhand.vo.publicapi;

public record CommentAuthorResponse(
        Long userId,
        String realName,
        String avatarUrl
) {
}