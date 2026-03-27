package com.campus.secondhand.vo.user;

import java.time.LocalDateTime;

public record UserNotificationResponse(
        Long notificationId,
        String businessType,
        Long businessId,
        String title,
        String content,
        String sendStatus,
        LocalDateTime sentAt,
        LocalDateTime readAt,
        LocalDateTime createdAt,
        Boolean read
) {
}
