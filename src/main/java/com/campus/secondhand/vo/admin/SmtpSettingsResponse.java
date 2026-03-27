package com.campus.secondhand.vo.admin;

import java.time.LocalDateTime;

public record SmtpSettingsResponse(
        boolean enabled,
        String host,
        Integer port,
        String username,
        String fromAddress,
        boolean authEnabled,
        boolean starttlsEnabled,
        boolean sslEnabled,
        boolean passwordConfigured,
        boolean smtpReady,
        LocalDateTime updatedAt,
        Long updatedByAdminId
) {
}