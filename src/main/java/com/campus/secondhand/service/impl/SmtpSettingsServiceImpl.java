package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.admin.SendSmtpTestEmailRequest;
import com.campus.secondhand.dto.admin.UpdateSmtpSettingsRequest;
import com.campus.secondhand.entity.AdminOperationLog;
import com.campus.secondhand.entity.SystemSetting;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.SystemSettingMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.SmtpMailSenderFactory;
import com.campus.secondhand.service.SmtpRuntimeSettings;
import com.campus.secondhand.service.SmtpSettingsService;
import com.campus.secondhand.vo.admin.SmtpSettingsResponse;
import com.campus.secondhand.vo.admin.SmtpTestResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SmtpSettingsServiceImpl implements SmtpSettingsService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String KEY_ENABLED = "smtp_enabled";
    private static final String KEY_HOST = "smtp_host";
    private static final String KEY_PORT = "smtp_port";
    private static final String KEY_USERNAME = "smtp_username";
    private static final String KEY_PASSWORD = "smtp_password";
    private static final String KEY_FROM_ADDRESS = "smtp_from_address";
    private static final String KEY_AUTH_ENABLED = "smtp_auth_enabled";
    private static final String KEY_STARTTLS_ENABLED = "smtp_starttls_enabled";
    private static final String KEY_SSL_ENABLED = "smtp_ssl_enabled";
    private static final String SYSTEM_MAIL_DISPLAY_NAME = "校园二手交易管理系统";
    private static final String MAIL_SUBJECT_PREFIX = "【校园二手交易管理系统】";

    private final SystemSettingMapper systemSettingMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final SmtpMailSenderFactory smtpMailSenderFactory;

    public SmtpSettingsServiceImpl(SystemSettingMapper systemSettingMapper,
                                   AdminOperationLogMapper adminOperationLogMapper,
                                   SmtpMailSenderFactory smtpMailSenderFactory) {
        this.systemSettingMapper = systemSettingMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.smtpMailSenderFactory = smtpMailSenderFactory;
    }

    @Override
    public SmtpSettingsResponse getSettings() {
        SettingBundle bundle = loadBundle();
        return toResponse(bundle);
    }

    @Override
    @Transactional
    public SmtpSettingsResponse updateSettings(UpdateSmtpSettingsRequest request, AdminPrincipal principal, String ipAddress) {
        if (principal == null) {
            throw new BusinessException(40100, HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        SettingBundle current = loadBundle();
        boolean enabled = request.enabled() != null ? request.enabled() : current.enabled();
        String host = trimToNull(request.host());
        Integer port = request.port() != null ? request.port() : current.port();
        String username = trimToNull(request.username());
        String fromAddress = trimToNull(request.fromAddress());
        boolean authEnabled = request.authEnabled() != null ? request.authEnabled() : current.authEnabled();
        boolean starttlsEnabled = request.starttlsEnabled() != null ? request.starttlsEnabled() : current.starttlsEnabled();
        boolean sslEnabled = request.sslEnabled() != null ? request.sslEnabled() : current.sslEnabled();
        String password = request.password() == null || request.password().isBlank()
                ? current.password()
                : request.password().trim();

        if (enabled) {
            if (!StringUtils.hasText(host)) {
                throw new BusinessException(40041, HttpStatus.BAD_REQUEST, "SMTP host is required when SMTP is enabled");
            }
            if (port == null || port <= 0) {
                throw new BusinessException(40042, HttpStatus.BAD_REQUEST, "SMTP port must be a positive number");
            }
            if (authEnabled && !StringUtils.hasText(username)) {
                throw new BusinessException(40043, HttpStatus.BAD_REQUEST, "SMTP username is required when authentication is enabled");
            }
            if (authEnabled && !StringUtils.hasText(password)) {
                throw new BusinessException(40044, HttpStatus.BAD_REQUEST, "SMTP password is required when authentication is enabled");
            }
            if (!StringUtils.hasText(fromAddress)) {
                fromAddress = username;
            }
            if (!StringUtils.hasText(fromAddress)) {
                throw new BusinessException(40045, HttpStatus.BAD_REQUEST, "SMTP sender address is required");
            }
        }

        upsert(KEY_ENABLED, Boolean.toString(enabled), "boolean", principal.getAdminId());
        upsert(KEY_HOST, host, "string", principal.getAdminId());
        upsert(KEY_PORT, port == null ? null : Integer.toString(port), "integer", principal.getAdminId());
        upsert(KEY_USERNAME, username, "string", principal.getAdminId());
        upsert(KEY_PASSWORD, password, "string", principal.getAdminId());
        upsert(KEY_FROM_ADDRESS, fromAddress, "string", principal.getAdminId());
        upsert(KEY_AUTH_ENABLED, Boolean.toString(authEnabled), "boolean", principal.getAdminId());
        upsert(KEY_STARTTLS_ENABLED, Boolean.toString(starttlsEnabled), "boolean", principal.getAdminId());
        upsert(KEY_SSL_ENABLED, Boolean.toString(sslEnabled), "boolean", principal.getAdminId());

        logOperation(principal.getAdminId(), 0L, "edit", buildOperationDetail(host, port, username, enabled), ipAddress);
        return getSettings();
    }

    @Override
    public SmtpTestResponse sendTestEmail(SendSmtpTestEmailRequest request, AdminPrincipal principal, String ipAddress) {
        if (principal == null) {
            throw new BusinessException(40100, HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        SmtpRuntimeSettings settings = getRuntimeSettings();
        if (settings == null) {
            throw new BusinessException(40046, HttpStatus.BAD_REQUEST, "SMTP configuration is incomplete or disabled");
        }

        String subject = normalizeSubject(request.subject());
        try {
            JavaMailSender mailSender = smtpMailSenderFactory.createSender(settings);
            sendUtf8Mail(mailSender, settings.fromAddress(), request.toEmail(), subject, request.content());
            logOperation(principal.getAdminId(), 0L, "other", "{\"to\":\"" + request.toEmail() + "\"}", ipAddress);
            return new SmtpTestResponse(request.toEmail(), subject, LocalDateTime.now());
        } catch (Exception ex) {
            throw new BusinessException(50041, HttpStatus.INTERNAL_SERVER_ERROR, "SMTP test email failed: " + ex.getMessage());
        }
    }

    @Override
    public SmtpRuntimeSettings getRuntimeSettings() {
        SettingBundle bundle = loadBundle();
        if (!bundle.enabled() || !bundle.smtpReady()) {
            return null;
        }
        return new SmtpRuntimeSettings(
                bundle.host(),
                bundle.port(),
                bundle.username(),
                bundle.password(),
                bundle.fromAddress(),
                bundle.authEnabled(),
                bundle.starttlsEnabled(),
                bundle.sslEnabled()
        );
    }

    private SettingBundle loadBundle() {
        boolean enabled = readBoolean(KEY_ENABLED, false);
        String host = trimToNull(readValue(KEY_HOST));
        Integer port = readInteger(KEY_PORT, 587);
        String username = trimToNull(readValue(KEY_USERNAME));
        String password = trimToNull(readValue(KEY_PASSWORD));
        String fromAddress = trimToNull(readValue(KEY_FROM_ADDRESS));
        boolean authEnabled = readBoolean(KEY_AUTH_ENABLED, true);
        boolean starttlsEnabled = readBoolean(KEY_STARTTLS_ENABLED, true);
        boolean sslEnabled = readBoolean(KEY_SSL_ENABLED, false);
        SystemSetting lastUpdated = newestSetting();
        boolean smtpReady = enabled
                && StringUtils.hasText(host)
                && port != null
                && port > 0
                && (!authEnabled || (StringUtils.hasText(username) && StringUtils.hasText(password)))
                && StringUtils.hasText(StringUtils.hasText(fromAddress) ? fromAddress : username);
        return new SettingBundle(
                enabled,
                host,
                port,
                username,
                password,
                StringUtils.hasText(fromAddress) ? fromAddress : username,
                authEnabled,
                starttlsEnabled,
                sslEnabled,
                smtpReady,
                lastUpdated == null ? null : lastUpdated.getUpdatedAt(),
                lastUpdated == null ? null : lastUpdated.getUpdatedByAdminId()
        );
    }

    private SmtpSettingsResponse toResponse(SettingBundle bundle) {
        return new SmtpSettingsResponse(
                bundle.enabled(),
                bundle.host(),
                bundle.port(),
                bundle.username(),
                bundle.fromAddress(),
                bundle.authEnabled(),
                bundle.starttlsEnabled(),
                bundle.sslEnabled(),
                StringUtils.hasText(bundle.password()),
                bundle.smtpReady(),
                bundle.updatedAt(),
                bundle.updatedByAdminId()
        );
    }

    private void upsert(String key, String value, String valueType, Long adminId) {
        SystemSetting existing = systemSettingMapper.selectBySettingKey(key);
        if (existing == null) {
            systemSettingMapper.insert(SystemSetting.builder()
                    .settingKey(key)
                    .settingValue(value)
                    .valueType(valueType)
                    .updatedByAdminId(adminId)
                    .build());
            return;
        }
        existing.setSettingValue(value);
        existing.setValueType(valueType);
        existing.setUpdatedByAdminId(adminId);
        systemSettingMapper.updateById(existing);
    }

    private String readValue(String key) {
        SystemSetting setting = systemSettingMapper.selectBySettingKey(key);
        return setting == null ? null : setting.getSettingValue();
    }

    private boolean readBoolean(String key, boolean defaultValue) {
        String value = readValue(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    private Integer readInteger(String key, Integer defaultValue) {
        String value = trimToNull(readValue(key));
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private SystemSetting newestSetting() {
        SystemSetting newest = null;
        for (String key : new String[]{KEY_ENABLED, KEY_HOST, KEY_PORT, KEY_USERNAME, KEY_PASSWORD, KEY_FROM_ADDRESS, KEY_AUTH_ENABLED, KEY_STARTTLS_ENABLED, KEY_SSL_ENABLED}) {
            SystemSetting setting = systemSettingMapper.selectBySettingKey(key);
            if (setting != null && (newest == null || (setting.getUpdatedAt() != null && newest.getUpdatedAt() != null && setting.getUpdatedAt().isAfter(newest.getUpdatedAt())) || newest.getUpdatedAt() == null)) {
                newest = setting;
            }
        }
        return newest;
    }

    private void logOperation(Long adminId, Long targetId, String operationType, String operationDetail, String ipAddress) {
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminId(adminId)
                .targetType("system_setting")
                .targetId(targetId)
                .operationType(operationType)
                .operationDetail(operationDetail)
                .ipAddress(ipAddress)
                .build());
    }

    private String buildOperationDetail(String host, Integer port, String username, boolean enabled) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("enabled", enabled);
        detail.put("host", host);
        detail.put("port", port);
        detail.put("username", username);
        try {
            return OBJECT_MAPPER.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize SMTP operation detail", ex);
        }
    }

    private void sendUtf8Mail(JavaMailSender mailSender,
                              String fromAddress,
                              String toAddress,
                              String subject,
                              String content) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
        helper.setTo(toAddress);
        helper.setFrom(new InternetAddress(fromAddress, SYSTEM_MAIL_DISPLAY_NAME, StandardCharsets.UTF_8.name()));
        helper.setSubject(subject);
        helper.setText(content, false);
        mailSender.send(mimeMessage);
    }

    private String normalizeSubject(String subject) {
        String normalized = trimToNull(subject);
        if (!StringUtils.hasText(normalized)) {
            return MAIL_SUBJECT_PREFIX + "SMTP 测试邮件";
        }
        if (normalized.startsWith(MAIL_SUBJECT_PREFIX)) {
            return normalized;
        }
        return MAIL_SUBJECT_PREFIX + normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record SettingBundle(
            boolean enabled,
            String host,
            Integer port,
            String username,
            String password,
            String fromAddress,
            boolean authEnabled,
            boolean starttlsEnabled,
            boolean sslEnabled,
            boolean smtpReady,
            LocalDateTime updatedAt,
            Long updatedByAdminId
    ) {
    }
}