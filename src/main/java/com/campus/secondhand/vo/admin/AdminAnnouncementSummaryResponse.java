package com.campus.secondhand.vo.admin;

import java.time.LocalDateTime;

public record AdminAnnouncementSummaryResponse(
        Long announcementId,
        String title,
        Boolean pinned,
        String publishStatus,
        LocalDateTime publishedAt,
        LocalDateTime expireAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
