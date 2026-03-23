package com.campus.secondhand.service.impl;

import com.campus.secondhand.entity.Notification;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.NotificationBusinessType;
import com.campus.secondhand.enums.NotificationChannel;
import com.campus.secondhand.enums.NotificationSendStatus;
import com.campus.secondhand.mapper.NotificationMapper;
import com.campus.secondhand.service.NotificationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailUsername;

    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                   ObjectProvider<JavaMailSender> mailSenderProvider,
                                   @Value("${spring.mail.username:}") String mailUsername) {
        this.notificationMapper = notificationMapper;
        this.mailSenderProvider = mailSenderProvider;
        this.mailUsername = mailUsername;
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
                application.getApplicationId(),
                "Registration rejected",
                content
        );
    }

    private void createEmailNotification(Long receiverUserId,
                                         String receiverEmail,
                                         Long senderAdminId,
                                         Long businessId,
                                         String title,
                                         String content) {
        Notification notification = Notification.builder()
                .receiverUserId(receiverUserId)
                .receiverEmail(receiverEmail)
                .senderAdminId(senderAdminId)
                .channel(NotificationChannel.EMAIL)
                .businessType(NotificationBusinessType.REGISTER_REVIEW)
                .businessId(businessId)
                .title(title)
                .content(content)
                .sendStatus(NotificationSendStatus.PENDING)
                .build();

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(receiverEmail);
                if (!mailUsername.isBlank()) {
                    message.setFrom(mailUsername);
                }
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