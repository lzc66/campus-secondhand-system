package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.secondhand.entity.Notification;
import com.campus.secondhand.enums.NotificationChannel;
import com.campus.secondhand.enums.NotificationSendStatus;
import com.campus.secondhand.mapper.NotificationMapper;
import com.campus.secondhand.service.EmailDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件兜底重投:每 5 分钟扫描一次,把滞留在 PENDING(如当时 SMTP 未配置)或 FAILED(临时故障)
 * 且未超过最大重试次数的邮件重新交给异步分发。
 */
@Slf4j
@Component
public class EmailRetryJob {

    private static final int MAX_RETRY_COUNT = 3;
    private static final int STALE_MINUTES = 2;

    private final NotificationMapper notificationMapper;
    private final EmailDispatchService emailDispatchService;

    public EmailRetryJob(NotificationMapper notificationMapper, EmailDispatchService emailDispatchService) {
        this.notificationMapper = notificationMapper;
        this.emailDispatchService = emailDispatchService;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void retryStaleEmails() {
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(STALE_MINUTES);
        List<Notification> pending = notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getChannel, NotificationChannel.EMAIL)
                .in(Notification::getSendStatus, NotificationSendStatus.PENDING, NotificationSendStatus.FAILED)
                .lt(Notification::getCreatedAt, staleBefore)
                .lt(Notification::getRetryCount, MAX_RETRY_COUNT));
        for (Notification notification : pending) {
            emailDispatchService.send(notification.getNotificationId());
        }
        if (!pending.isEmpty()) {
            log.info("EmailRetryJob resubmitted {} pending/failed email notification(s)", pending.size());
        }
    }
}
