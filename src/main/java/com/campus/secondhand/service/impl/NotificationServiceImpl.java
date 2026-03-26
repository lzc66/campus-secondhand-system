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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

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
                .title("Registration approved")
                .content("Your registration application has been approved. You can now sign in with your student number.")
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
                "Registration approved",
                "Your registration application has been approved. You can now sign in with your student number and password."
        );
    }

    @Override
    public void sendRegistrationRejected(RegistrationApplication application, Long adminId) {
        String content = application.getReviewRemark() == null || application.getReviewRemark().isBlank()
                ? "Your registration application has been rejected. You can submit a new application later."
                : "Your registration application has been rejected. Remark: " + application.getReviewRemark();
        createEmailNotification(
                null,
                application.getEmail(),
                adminId,
                NotificationBusinessType.REGISTER_REVIEW,
                application.getApplicationId(),
                "Registration rejected",
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
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(receiverEmail);
                message.setFrom(settings.fromAddress());
                message.setSubject(title);
                message.setText(content);
                mailSender.send(message);
                notification.setSendStatus(NotificationSendStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } catch (Exception ex) {
                notification.setSendStatus(NotificationSendStatus.FAILED);
            }
        }

        notificationMapper.insert(notification);
    }
}