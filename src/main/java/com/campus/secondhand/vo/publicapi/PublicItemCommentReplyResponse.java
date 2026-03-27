package com.campus.secondhand.vo.publicapi;

import java.time.LocalDateTime;

public record PublicItemCommentReplyResponse(
        Long commentId,
        Long itemId,
        String content,
        LocalDateTime createdAt,
        Long replyToUserId,
        String replyToUserName,
        CommentAuthorResponse author
) {
}