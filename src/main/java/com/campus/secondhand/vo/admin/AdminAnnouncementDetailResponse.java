package com.campus.secondhand.vo.admin;

import java.time.LocalDateTime;

public record AdminAnnouncementDetailResponse(
        Long announcementId,
        Long publisherAdminId,
        String title,
        String content,
        Boolean pinned,
        String publishStatus,
        LocalDateTime publishedAt,
        LocalDateTime expireAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
