package com.campus.secondhand.service;

public record SmtpRuntimeSettings(
        String host,
        int port,
        String username,
        String password,
        String fromAddress,
        boolean authEnabled,
        boolean starttlsEnabled,
        boolean sslEnabled
) {
}