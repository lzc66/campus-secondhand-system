package com.campus.secondhand.dto.admin;

public record UpdateSmtpSettingsRequest(
        Boolean enabled,
        String host,
        Integer port,
        String username,
        String password,
        String fromAddress,
        Boolean authEnabled,
        Boolean starttlsEnabled,
        Boolean sslEnabled
) {
}