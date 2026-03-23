package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.entity.Notification;
import com.campus.secondhand.enums.NotificationChannel;
import com.campus.secondhand.mapper.NotificationMapper;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.UserNotificationCenterService;
import com.campus.secondhand.vo.user.UserNotificationPageResponse;
import com.campus.secondhand.vo.user.UserNotificationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class UserNotificationCenterServiceImpl implements UserNotificationCenterService {

    private final NotificationMapper notificationMapper;

    public UserNotificationCenterServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public UserNotificationPageResponse listNotifications(UserPrincipal principal, String readStatus, long page, long size) {
        Boolean unreadOnly = parseReadStatus(readStatus);
        Page<Notification> queryPage = new Page<>(Math.max(page, 1), Math.max(size, 1));
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverUserId, principal.getUserId())
                .eq(Notification::getChannel, NotificationChannel.SITE)
                .isNull(Boolean.TRUE.equals(unreadOnly), Notification::getReadAt)
                .isNotNull(Boolean.FALSE.equals(unreadOnly), Notification::getReadAt)
                .orderByDesc(Notification::getCreatedAt);
        Page<Notification> result = notificationMapper.selectPage(queryPage, wrapper);
        Long unreadCount = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverUserId, principal.getUserId())
                .eq(Notification::getChannel, NotificationChannel.SITE)
                .isNull(Notification::getReadAt));
        List<UserNotificationResponse> records = result.getRecords().stream().map(this::toResponse).toList();
        return new UserNotificationPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), unreadCount == null ? 0 : unreadCount, records);
    }

    @Override
    @Transactional
    public UserNotificationResponse markRead(UserPrincipal principal, Long notificationId) {
        Notification notification = getRequiredSiteNotification(principal.getUserId(), notificationId);
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(notification);
        }
        return toResponse(notification);
    }

    @Override
    @Transactional
    public long markAllRead(UserPrincipal principal) {
        List<Notification> notifications = notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverUserId, principal.getUserId())
                .eq(Notification::getChannel, NotificationChannel.SITE)
                .isNull(Notification::getReadAt));
        LocalDateTime now = LocalDateTime.now();
        notifications.forEach(notification -> {
            notification.setReadAt(now);
            notificationMapper.updateById(notification);
        });
        return notifications.size();
    }

    private Notification getRequiredSiteNotification(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || notification.getChannel() != NotificationChannel.SITE || !userId.equals(notification.getReceiverUserId())) {
            throw new BusinessException(40491, HttpStatus.NOT_FOUND, "Notification not found");
        }
        return notification;
    }

    private Boolean parseReadStatus(String readStatus) {
        if (!StringUtils.hasText(readStatus)) {
            return null;
        }
        return switch (readStatus.trim().toLowerCase(Locale.ROOT)) {
            case "unread" -> true;
            case "read" -> false;
            default -> throw new BusinessException(40092, HttpStatus.BAD_REQUEST, "readStatus must be unread or read");
        };
    }

    private UserNotificationResponse toResponse(Notification notification) {
        return new UserNotificationResponse(
                notification.getNotificationId(),
                notification.getBusinessType().getValue(),
                notification.getBusinessId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getSendStatus().getValue(),
                notification.getSentAt(),
                notification.getReadAt(),
                notification.getCreatedAt(),
                notification.getReadAt() != null
        );
    }
}
