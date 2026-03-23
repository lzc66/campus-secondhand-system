package com.campus.secondhand.vo.admin;

import java.time.LocalDateTime;

public record InitStatusResponse(
        long categoryCount,
        boolean defaultAdminExists,
        boolean currentAdminSuperAdmin,
        LocalDateTime lastBootstrapAt
) {
}
