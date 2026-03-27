package com.campus.secondhand.vo.user;

import com.campus.secondhand.vo.publicapi.CommentAuthorResponse;

import java.time.LocalDateTime;

public record UserReceivedCommentResponse(
        Long commentId,
        Long itemId,
        String itemTitle,
        String content,
        LocalDateTime createdAt,
        CommentAuthorResponse author,
        boolean replied
) {
}