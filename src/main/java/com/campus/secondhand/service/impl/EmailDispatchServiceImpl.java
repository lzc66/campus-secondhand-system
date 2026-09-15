package com.campus.secondhand.service.impl;

import com.campus.secondhand.entity.Notification;
import com.campus.secondhand.enums.NotificationChannel;
import com.campus.secondhand.enums.NotificationSendStatus;
import com.campus.secondhand.mapper.NotificationMapper;
import com.campus.secondhand.service.EmailDispatchService;
import com.campus.secondhand.service.SmtpMailSenderFactory;
import com.campus.secondhand.service.SmtpRuntimeSettings;
import com.campus.secondhand.service.SmtpSettingsService;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 邮件异步分发:通知记录先以 PENDING 落库,再由本服务在事务外异步发送,
 * 成功置 SENT、失败置 FAILED 并累加重试次数(由 EmailRetryJob 兜底重投)。
 */
@Slf4j
@Service
public class EmailDispatchServiceImpl implements EmailDispatchService {

    private static final String SYSTEM_MAIL_DISPLAY_NAME = "校园二手交易管理系统";

    private final NotificationMapper notificationMapper;
    private final SmtpSettingsService smtpSettingsService;
    private final SmtpMailSenderFactory smtpMailSenderFactory;

    public EmailDispatchServiceImpl(NotificationMapper notificationMapper,
                                    SmtpSettingsService smtpSettingsService,
                                    SmtpMailSenderFactory smtpMailSenderFactory) {
        this.notificationMapper = notificationMapper;
        this.smtpSettingsService = smtpSettingsService;
        this.smtpMailSenderFactory = smtpMailSenderFactory;
    }

    @Override
    @Async("emailExecutor")
    public void send(Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || notification.getChannel() != NotificationChannel.EMAIL) {
            return;
        }
        if (notification.getSendStatus() == NotificationSendStatus.SENT) {
            return;
        }
        if (!StringUtils.hasText(notification.getReceiverEmail())) {
            markFailed(notification);
            return;
        }
        SmtpRuntimeSettings settings = smtpSettingsService.getRuntimeSettings();
        if (settings == null) {
            // SMTP 尚未配置/未启用:保持 PENDING,由重试任务在配置完成后补发
            return;
        }
        try {
            JavaMailSender mailSender = smtpMailSenderFactory.createSender(settings);
            sendUtf8Mail(mailSender, settings.fromAddress(), notification.getReceiverEmail(), notification.getTitle(), notification.getContent());
            notification.setSendStatus(NotificationSendStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationMapper.updateById(notification);
        } catch (Exception ex) {
            log.warn("Email delivery failed for notification {}: {}", notification.getNotificationId(), ex.getMessage());
            markFailed(notification);
        }
    }

    private void markFailed(Notification notification) {
        notification.setSendStatus(NotificationSendStatus.FAILED);
        notification.setRetryCount((notification.getRetryCount() == null ? 0 : notification.getRetryCount()) + 1);
        notificationMapper.updateById(notification);
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
}
