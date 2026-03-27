package com.campus.secondhand.service;

import com.campus.secondhand.entity.Announcement;
import com.campus.secondhand.entity.Notification;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.NotificationSendStatus;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.mapper.NotificationMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.service.impl.NotificationServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SmtpSettingsService smtpSettingsService;
    @Mock
    private SmtpMailSenderFactory smtpMailSenderFactory;
    @Mock
    private JavaMailSender javaMailSender;

    @Test
    void shouldKeepEmailNotificationPendingWhenSmtpDisabled() {
        when(smtpSettingsService.getRuntimeSettings()).thenReturn(null);
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, userMapper, smtpSettingsService, smtpMailSenderFactory);

        service.sendRegistrationRejected(RegistrationApplication.builder()
                .applicationId(5L)
                .email("alice@campus.local")
                .build(), 1L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals(NotificationSendStatus.PENDING, captor.getValue().getSendStatus());
    }

    @Test
    void shouldMarkEmailNotificationSentWhenSmtpConfigured() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(smtpSettingsService.getRuntimeSettings()).thenReturn(new SmtpRuntimeSettings(
                "smtp.example.com", 587, "mailer@example.com", "secret", "mailer@example.com", true, true, false
        ));
        when(smtpMailSenderFactory.createSender(any())).thenReturn(javaMailSender);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, userMapper, smtpSettingsService, smtpMailSenderFactory);

        service.sendRegistrationRejected(RegistrationApplication.builder()
                .applicationId(5L)
                .email("alice@campus.local")
                .reviewRemark("学生证信息清晰，请重新提交后等待复核")
                .build(), 1L);

        verify(javaMailSender).send(any(MimeMessage.class));
        assertTrue(String.valueOf(mimeMessage.getDataHandler().getContentType()).toLowerCase().contains("charset=utf-8"));
        assertEquals("【校园二手交易管理系统】注册审核结果通知", mimeMessage.getSubject());
        InternetAddress from = (InternetAddress) mimeMessage.getFrom()[0];
        assertEquals("mailer@example.com", from.getAddress());
        assertEquals("校园二手交易管理系统", from.getPersonal());
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals(NotificationSendStatus.SENT, captor.getValue().getSendStatus());
        assertTrue(new String(mimeMessage.getInputStream().readAllBytes(), StandardCharsets.UTF_8).contains("审核备注"));
    }

    @Test
    void shouldInsertSiteNotificationsForPublishedAnnouncement() {
        when(userMapper.selectList(any())).thenReturn(List.of(
                User.builder().userId(11L).email("alice@campus.local").accountStatus(UserAccountStatus.ACTIVE).build(),
                User.builder().userId(22L).email("bob@campus.local").accountStatus(UserAccountStatus.ACTIVE).build()
        ));
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, userMapper, smtpSettingsService, smtpMailSenderFactory);

        service.sendAnnouncementPublished(Announcement.builder()
                .announcementId(7L)
                .title("System Notice")
                .content("Dorm delivery starts at 7pm.")
                .build(), 1L);

        verify(notificationMapper, atLeastOnce()).insert(any(Notification.class));
    }
}