package com.campus.secondhand.vo.publicapi;

import java.time.LocalDateTime;

public record PublicAnnouncementResponse(
        Long announcementId,
        String title,
        String content,
        Boolean pinned,
        LocalDateTime publishedAt,
        LocalDateTime expireAt
) {
}
