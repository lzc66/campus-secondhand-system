package com.campus.secondhand.service;

import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.vo.user.UserNotificationPageResponse;
import com.campus.secondhand.vo.user.UserNotificationResponse;

public interface UserNotificationCenterService {

    UserNotificationPageResponse listNotifications(UserPrincipal principal, String readStatus, long page, long size);

    UserNotificationResponse markRead(UserPrincipal principal, Long notificationId);

    long markAllRead(UserPrincipal principal);
}
