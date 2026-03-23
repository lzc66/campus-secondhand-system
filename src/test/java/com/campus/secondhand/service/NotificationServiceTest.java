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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Test
    void shouldKeepEmailNotificationPendingWhenMailSenderMissing() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, userMapper, mailSenderProvider, "");

        service.sendRegistrationRejected(RegistrationApplication.builder()
                .applicationId(5L)
                .email("alice@campus.local")
                .build(), 1L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals(NotificationSendStatus.PENDING, captor.getValue().getSendStatus());
    }

    @Test
    void shouldInsertSiteNotificationsForPublishedAnnouncement() {
        when(userMapper.selectList(any())).thenReturn(List.of(
                User.builder().userId(11L).email("alice@campus.local").accountStatus(UserAccountStatus.ACTIVE).build(),
                User.builder().userId(22L).email("bob@campus.local").accountStatus(UserAccountStatus.ACTIVE).build()
        ));
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, userMapper, mailSenderProvider, "");

        service.sendAnnouncementPublished(Announcement.builder()
                .announcementId(7L)
                .title("System Notice")
                .content("Dorm delivery starts at 7pm.")
                .build(), 1L);

        verify(notificationMapper, atLeastOnce()).insert(any(Notification.class));
    }
}
