package com.campus.secondhand.service;

import com.campus.secondhand.entity.Notification;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.enums.NotificationSendStatus;
import com.campus.secondhand.mapper.NotificationMapper;
import com.campus.secondhand.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Test
    void shouldKeepEmailNotificationPendingWhenMailSenderMissing() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, mailSenderProvider, "");

        service.sendRegistrationRejected(RegistrationApplication.builder()
                .applicationId(5L)
                .email("alice@campus.local")
                .build(), 1L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals(NotificationSendStatus.PENDING, captor.getValue().getSendStatus());
    }
}