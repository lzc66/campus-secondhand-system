package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.secondhand.entity.Announcement;
import com.campus.secondhand.entity.Notification;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.NotificationBusinessType;
import com.campus.secondhand.enums.NotificationChannel;
import com.campus.secondhand.enums.NotificationSendStatus;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.mapper.NotificationMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.service.NotificationService;
import com.campus.secondhand.service.SmtpMailSenderFactory;
import com.campus.secondhand.service.SmtpRuntimeSettings;
import com.campus.secondhand.service.SmtpSettingsService;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final String SYSTEM_MAIL_DISPLAY_NAME = "校园二手交易管理系统";
    private static final String MAIL_SUBJECT_PREFIX = "【校园二手交易管理系统】";

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final SmtpSettingsService smtpSettingsService;
    private final SmtpMailSenderFactory smtpMailSenderFactory;

    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                   UserMapper userMapper,
                                   SmtpSettingsService smtpSettingsService,
                                   SmtpMailSenderFactory smtpMailSenderFactory) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.smtpSettingsService = smtpSettingsService;
        this.smtpMailSenderFactory = smtpMailSenderFactory;
    }

    @Override
    public void sendRegistrationApproved(RegistrationApplication application, User user, Long adminId) {
        Notification siteNotification = Notification.builder()
                .receiverUserId(user.getUserId())
                .receiverEmail(user.getEmail())
                .senderAdminId(adminId)
                .channel(NotificationChannel.SITE)
                .businessType(NotificationBusinessType.REGISTER_REVIEW)
                .businessId(application.getApplicationId())
                .title("注册审核已通过")
                .content("你的注册申请已通过审核，现在可以使用学号和密码登录系统。")
                .sendStatus(NotificationSendStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();
        notificationMapper.insert(siteNotification);

        createEmailNotification(
                user.getUserId(),
                user.getEmail(),
                adminId,
                NotificationBusinessType.REGISTER_REVIEW,
                application.getApplicationId(),
                buildMailSubject("注册审核通过通知"),
                "你的注册申请已通过审核，现在可以使用学号和密码登录系统。"
        );
    }

    @Override
    public void sendRegistrationRejected(RegistrationApplication application, Long adminId) {
        String content = application.getReviewRemark() == null || application.getReviewRemark().isBlank()
                ? "你的注册申请未通过审核，可在完善资料后重新提交。"
                : "你的注册申请未通过审核。审核备注：" + application.getReviewRemark();
        createEmailNotification(
                null,
                application.getEmail(),
                adminId,
                NotificationBusinessType.REGISTER_REVIEW,
                application.getApplicationId(),
                buildMailSubject("注册审核结果通知"),
                content
        );
    }

    @Override
    public void sendAnnouncementPublished(Announcement announcement, Long adminId) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getAccountStatus, UserAccountStatus.ACTIVE)
                .isNull(User::getDeletedAt));
        LocalDateTime now = LocalDateTime.now();
        for (User user : users) {
            notificationMapper.insert(Notification.builder()
                    .receiverUserId(user.getUserId())
                    .receiverEmail(user.getEmail())
                    .senderAdminId(adminId)
                    .channel(NotificationChannel.SITE)
                    .businessType(NotificationBusinessType.ANNOUNCEMENT)
                    .businessId(announcement.getAnnouncementId())
                    .title(announcement.getTitle())
                    .content(announcement.getContent())
                    .sendStatus(NotificationSendStatus.SENT)
                    .sentAt(now)
                    .build());
        }
    }

    private void createEmailNotification(Long receiverUserId,
                                         String receiverEmail,
                                         Long senderAdminId,
                                         NotificationBusinessType businessType,
                                         Long businessId,
                                         String title,
                                         String content) {
        Notification notification = Notification.builder()
                .receiverUserId(receiverUserId)
                .receiverEmail(receiverEmail)
                .senderAdminId(senderAdminId)
                .channel(NotificationChannel.EMAIL)
                .businessType(businessType)
                .businessId(businessId)
                .title(title)
                .content(content)
                .sendStatus(NotificationSendStatus.PENDING)
                .build();

        SmtpRuntimeSettings settings = smtpSettingsService.getRuntimeSettings();
        if (settings != null) {
            try {
                JavaMailSender mailSender = smtpMailSenderFactory.createSender(settings);
                sendUtf8Mail(mailSender, settings.fromAddress(), receiverEmail, title, content);
                notification.setSendStatus(NotificationSendStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } catch (Exception ex) {
                notification.setSendStatus(NotificationSendStatus.FAILED);
            }
        }

        notificationMapper.insert(notification);
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

    private String buildMailSubject(String coreTitle) {
        return MAIL_SUBJECT_PREFIX + coreTitle;
    }
}