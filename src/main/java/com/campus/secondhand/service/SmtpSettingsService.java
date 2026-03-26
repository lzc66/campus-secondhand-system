package com.campus.secondhand.service;

import com.campus.secondhand.dto.admin.SendSmtpTestEmailRequest;
import com.campus.secondhand.dto.admin.UpdateSmtpSettingsRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.vo.admin.SmtpSettingsResponse;
import com.campus.secondhand.vo.admin.SmtpTestResponse;

public interface SmtpSettingsService {

    SmtpSettingsResponse getSettings();

    SmtpSettingsResponse updateSettings(UpdateSmtpSettingsRequest request, AdminPrincipal principal, String ipAddress);

    SmtpTestResponse sendTestEmail(SendSmtpTestEmailRequest request, AdminPrincipal principal, String ipAddress);

    SmtpRuntimeSettings getRuntimeSettings();
}