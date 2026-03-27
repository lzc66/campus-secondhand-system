package com.campus.secondhand.vo.publicapi;

import java.util.List;

public record PublicItemCommentPageResponse(
        long current,
        long size,
        long total,
        List<PublicItemCommentResponse> records
) {
}