package com.campus.secondhand.service;

import com.campus.secondhand.entity.Notification;
import com.campus.secondhand.enums.NotificationChannel;
import com.campus.secondhand.enums.NotificationSendStatus;
import com.campus.secondhand.mapper.NotificationMapper;
import com.campus.secondhand.service.impl.EmailDispatchServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailDispatchServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private SmtpSettingsService smtpSettingsService;
    @Mock
    private SmtpMailSenderFactory smtpMailSenderFactory;
    @Mock
    private JavaMailSender javaMailSender;

    @Test
    void shouldMarkSentAfterSuccessfulDelivery() throws Exception {
        when(notificationMapper.selectById(5L)).thenReturn(Notification.builder()
                .notificationId(5L)
                .receiverEmail("alice@campus.local")
                .channel(NotificationChannel.EMAIL)
                .sendStatus(NotificationSendStatus.PENDING)
                .title("Subject")
                .content("Body")
                .build());
        when(smtpSettingsService.getRuntimeSettings()).thenReturn(new SmtpRuntimeSettings(
                "smtp.example.com", 587, "mailer@example.com", "secret", "mailer@example.com", true, true, false));
        when(smtpMailSenderFactory.createSender(any())).thenReturn(javaMailSender);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        EmailDispatchServiceImpl service = new EmailDispatchServiceImpl(notificationMapper, smtpSettingsService, smtpMailSenderFactory);
        service.send(5L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).updateById(captor.capture());
        assertEquals(NotificationSendStatus.SENT, captor.getValue().getSendStatus());
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void shouldMarkFailedAndIncrementRetryCountWhenDeliveryFails() throws Exception {
        when(notificationMapper.selectById(6L)).thenReturn(Notification.builder()
                .notificationId(6L)
                .receiverEmail("alice@campus.local")
                .channel(NotificationChannel.EMAIL)
                .sendStatus(NotificationSendStatus.PENDING)
                .retryCount(0)
                .title("Subject")
                .content("Body")
                .build());
        when(smtpSettingsService.getRuntimeSettings()).thenReturn(new SmtpRuntimeSettings(
                "smtp.example.com", 587, "mailer@example.com", "secret", "mailer@example.com", true, true, false));
        when(smtpMailSenderFactory.createSender(any())).thenReturn(javaMailSender);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("boom")).when(javaMailSender).send(any(MimeMessage.class));

        EmailDispatchServiceImpl service = new EmailDispatchServiceImpl(notificationMapper, smtpSettingsService, smtpMailSenderFactory);
        service.send(6L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).updateById(captor.capture());
        assertEquals(NotificationSendStatus.FAILED, captor.getValue().getSendStatus());
        assertEquals(1, captor.getValue().getRetryCount());
    }

    @Test
    void shouldKeepPendingWhenSmtpNotConfigured() {
        when(notificationMapper.selectById(7L)).thenReturn(Notification.builder()
                .notificationId(7L)
                .receiverEmail("alice@campus.local")
                .channel(NotificationChannel.EMAIL)
                .sendStatus(NotificationSendStatus.PENDING)
                .title("Subject")
                .content("Body")
                .build());
        when(smtpSettingsService.getRuntimeSettings()).thenReturn(null);

        EmailDispatchServiceImpl service = new EmailDispatchServiceImpl(notificationMapper, smtpSettingsService, smtpMailSenderFactory);
        service.send(7L);

        // 未配置 SMTP 时保持 PENDING,交给重试任务在配置完成后补发
        verify(notificationMapper, org.mockito.Mockito.never()).updateById(any(Notification.class));
    }
}
