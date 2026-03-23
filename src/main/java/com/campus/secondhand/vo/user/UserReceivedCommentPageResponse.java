package com.campus.secondhand.vo.user;

import java.util.List;

public record UserReceivedCommentPageResponse(
        long current,
        long size,
        long total,
        List<UserReceivedCommentResponse> records
) {
}