package com.campus.secondhand.vo.user;

import java.util.List;

public record UserNotificationPageResponse(
        long current,
        long size,
        long total,
        long unreadCount,
        List<UserNotificationResponse> records
) {
}
