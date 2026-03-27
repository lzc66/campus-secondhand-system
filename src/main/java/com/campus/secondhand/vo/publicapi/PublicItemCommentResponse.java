package com.campus.secondhand.vo.publicapi;

import java.time.LocalDateTime;
import java.util.List;

public record PublicItemCommentResponse(
        Long commentId,
        Long itemId,
        String content,
        LocalDateTime createdAt,
        CommentAuthorResponse author,
        List<PublicItemCommentReplyResponse> replies
) {
}