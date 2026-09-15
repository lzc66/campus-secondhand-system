package com.campus.secondhand.service;

import com.campus.secondhand.entity.Announcement;
import com.campus.secondhand.entity.Notification;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.NotificationChannel;
import com.campus.secondhand.enums.NotificationSendStatus;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.mapper.NotificationMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private EmailDispatchService emailDispatchService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void shouldInsertPendingEmailAndDispatchAsyncForRejectedRegistration() {
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(5L);
            return 1;
        }).when(notificationMapper).insert(any(Notification.class));

        notificationService.sendRegistrationRejected(RegistrationApplication.builder()
                .applicationId(5L)
                .email("alice@campus.local")
                .reviewRemark("资料不完整")
                .build(), 1L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals(NotificationChannel.EMAIL, captor.getValue().getChannel());
        assertEquals(NotificationSendStatus.PENDING, captor.getValue().getSendStatus());
        // 邮件记录落库后异步分发(不再在业务线程里同步发信)
        verify(emailDispatchService).send(5L);
    }

    @Test
    void shouldInsertSiteNotificationAndPendingEmailForApprovedRegistration() {
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(9L);
            return 1;
        }).when(notificationMapper).insert(any(Notification.class));

        notificationService.sendRegistrationApproved(RegistrationApplication.builder()
                        .applicationId(6L)
                        .build(),
                User.builder().userId(11L).email("alice@campus.local").build(), 1L);

        verify(notificationMapper, org.mockito.Mockito.times(2)).insert(any(Notification.class));
        verify(emailDispatchService).send(9L);
    }

    @Test
    void shouldInsertSiteAndEmailNotificationsForPublishedAnnouncement() {
        when(userMapper.selectList(any())).thenReturn(List.of(
                User.builder().userId(11L).email("alice@campus.local").accountStatus(UserAccountStatus.ACTIVE).build(),
                User.builder().userId(22L).accountStatus(UserAccountStatus.ACTIVE).build()
        ));
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(7L);
            return 1;
        }).when(notificationMapper).insert(any(Notification.class));

        notificationService.sendAnnouncementPublished(Announcement.builder()
                .announcementId(7L)
                .title("System Notice")
                .content("Dorm delivery starts at 7pm.")
                .build(), 1L);

        // 2 个站内信 + 1 个邮件记录(仅 alice 有邮箱)
        verify(notificationMapper, org.mockito.Mockito.times(3)).insert(any(Notification.class));
        verify(emailDispatchService).send(7L);
    }

    @Test
    void shouldNotifyReceiverForOrderStatusChange() {
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(3L);
            return 1;
        }).when(notificationMapper).insert(any(Notification.class));
        when(userMapper.selectById(11L)).thenReturn(User.builder().userId(11L).email("alice@campus.local").build());

        notificationService.sendOrderStatusChanged(
                com.campus.secondhand.entity.TradeOrder.builder().orderId(1L).orderNo("ORD1").build(),
                11L, null, com.campus.secondhand.enums.OrderStatus.PENDING_CONFIRM,
                com.campus.secondhand.enums.OrderStatus.AWAITING_DELIVERY, null);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(NotificationChannel.SITE, captor.getAllValues().get(0).getChannel());
        assertEquals(NotificationChannel.EMAIL, captor.getAllValues().get(1).getChannel());
        verify(emailDispatchService).send(anyLong());
        org.junit.jupiter.api.Assertions.assertTrue(captor.getAllValues().get(0).getContent().contains("ORD1"));
    }
}
