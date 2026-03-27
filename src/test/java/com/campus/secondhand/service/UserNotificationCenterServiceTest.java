package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.entity.Notification;
import com.campus.secondhand.enums.NotificationBusinessType;
import com.campus.secondhand.enums.NotificationChannel;
import com.campus.secondhand.enums.NotificationSendStatus;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.mapper.NotificationMapper;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.impl.UserNotificationCenterServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserNotificationCenterServiceTest {

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private UserNotificationCenterServiceImpl userNotificationCenterService;

    @Test
    void shouldListUnreadNotifications() {
        Page<Notification> pageResult = new Page<>(1, 10, 1);
        pageResult.setRecords(List.of(Notification.builder()
                .notificationId(5L)
                .receiverUserId(11L)
                .channel(NotificationChannel.SITE)
                .businessType(NotificationBusinessType.ANNOUNCEMENT)
                .title("System Notice")
                .content("Dorm delivery starts at 7pm.")
                .sendStatus(NotificationSendStatus.SENT)
                .build()));
        when(notificationMapper.selectPage(any(), any())).thenReturn(pageResult);
        when(notificationMapper.selectCount(any())).thenReturn(1L);

        var response = userNotificationCenterService.listNotifications(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", UserAccountStatus.ACTIVE),
                "unread",
                1,
                10
        );

        assertEquals(1, response.unreadCount());
        assertEquals("System Notice", response.records().get(0).title());
    }

    @Test
    void shouldMarkNotificationRead() {
        when(notificationMapper.selectById(5L)).thenReturn(Notification.builder()
                .notificationId(5L)
                .receiverUserId(11L)
                .channel(NotificationChannel.SITE)
                .businessType(NotificationBusinessType.ANNOUNCEMENT)
                .title("System Notice")
                .content("Dorm delivery starts at 7pm.")
                .sendStatus(NotificationSendStatus.SENT)
                .build());

        var response = userNotificationCenterService.markRead(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", UserAccountStatus.ACTIVE),
                5L
        );

        assertEquals(true, response.read());
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).updateById(captor.capture());
    }
}
