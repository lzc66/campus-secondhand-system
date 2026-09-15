package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.secondhand.entity.Announcement;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.ItemComment;
import com.campus.secondhand.entity.Notification;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.entity.TradeOrder;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.NotificationBusinessType;
import com.campus.secondhand.enums.NotificationChannel;
import com.campus.secondhand.enums.NotificationSendStatus;
import com.campus.secondhand.enums.OrderStatus;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.mapper.NotificationMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.service.EmailDispatchService;
import com.campus.secondhand.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final String MAIL_SUBJECT_PREFIX = "【校园二手交易管理系统】";

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final EmailDispatchService emailDispatchService;

    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                   UserMapper userMapper,
                                   EmailDispatchService emailDispatchService) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.emailDispatchService = emailDispatchService;
    }

    @Override
    public void sendRegistrationApproved(RegistrationApplication application, User user, Long adminId) {
        insertSiteNotification(user.getUserId(), user.getEmail(), adminId, NotificationBusinessType.REGISTER_REVIEW,
                application.getApplicationId(), "注册审核已通过",
                "你的注册申请已通过审核，现在可以使用学号和密码登录系统。");
        createEmailNotification(user.getUserId(), user.getEmail(), adminId, NotificationBusinessType.REGISTER_REVIEW,
                application.getApplicationId(), buildMailSubject("注册审核通过通知"),
                "你的注册申请已通过审核，现在可以使用学号和密码登录系统。");
    }

    @Override
    public void sendRegistrationRejected(RegistrationApplication application, Long adminId) {
        // 被驳回的申请人尚未成为系统用户,没有可接收站内信的账号,仅发邮件
        String content = application.getReviewRemark() == null || application.getReviewRemark().isBlank()
                ? "你的注册申请未通过审核，可在完善资料后重新提交。"
                : "你的注册申请未通过审核。审核备注：" + application.getReviewRemark();
        createEmailNotification(null, application.getEmail(), adminId, NotificationBusinessType.REGISTER_REVIEW,
                application.getApplicationId(), buildMailSubject("注册审核结果通知"), content);
    }

    @Override
    public void sendAnnouncementPublished(Announcement announcement, Long adminId) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getAccountStatus, UserAccountStatus.ACTIVE)
                .isNull(User::getDeletedAt));
        LocalDateTime now = LocalDateTime.now();
        for (User user : users) {
            insertSiteNotification(user.getUserId(), user.getEmail(), adminId, NotificationBusinessType.ANNOUNCEMENT,
                    announcement.getAnnouncementId(), announcement.getTitle(), announcement.getContent());
            // 公告邮件通道:先落 PENDING 再异步发送,不阻塞管理端事务
            if (StringUtils.hasText(user.getEmail())) {
                createEmailNotification(user.getUserId(), user.getEmail(), adminId, NotificationBusinessType.ANNOUNCEMENT,
                        announcement.getAnnouncementId(), buildMailSubject(announcement.getTitle()), announcement.getContent());
            }
        }
    }

    @Override
    public void sendOrderStatusChanged(TradeOrder order, Long receiverUserId, Long senderAdminId,
                                       OrderStatus fromStatus, OrderStatus toStatus, String note) {
        String content = "你的订单 " + order.getOrderNo() + " 状态已更新："
                + orderStatusLabel(fromStatus) + " → " + orderStatusLabel(toStatus)
                + (StringUtils.hasText(note) ? "。备注：" + note : "。");
        insertSiteNotification(receiverUserId, null, senderAdminId, NotificationBusinessType.ORDER_UPDATE,
                order.getOrderId(), "订单状态更新", content);
        // 邮件只发给有邮箱的用户
        User receiver = receiverUserId == null ? null : userMapper.selectById(receiverUserId);
        if (receiver != null && StringUtils.hasText(receiver.getEmail())) {
            createEmailNotification(receiver.getUserId(), receiver.getEmail(), senderAdminId,
                    NotificationBusinessType.ORDER_UPDATE, order.getOrderId(), buildMailSubject("订单状态更新"), content);
        }
    }

    @Override
    public void sendCommentReplied(Item item, ItemComment reply, Long receiverUserId) {
        String content = "你在商品「" + (item == null || item.getTitle() == null ? "未知商品" : item.getTitle())
                + "」的留言收到了回复：" + (reply == null ? "" : reply.getContent());
        insertSiteNotification(receiverUserId, null, null, NotificationBusinessType.COMMENT_REPLY,
                reply == null ? null : reply.getCommentId(), "收到新回复", content);
        User receiver = receiverUserId == null ? null : userMapper.selectById(receiverUserId);
        if (receiver != null && StringUtils.hasText(receiver.getEmail())) {
            createEmailNotification(receiver.getUserId(), receiver.getEmail(), null,
                    NotificationBusinessType.COMMENT_REPLY, reply == null ? null : reply.getCommentId(),
                    buildMailSubject("收到新回复"), content);
        }
    }

    private void insertSiteNotification(Long receiverUserId, String receiverEmail, Long senderAdminId,
                                        NotificationBusinessType businessType, Long businessId,
                                        String title, String content) {
        notificationMapper.insert(Notification.builder()
                .receiverUserId(receiverUserId)
                .receiverEmail(receiverEmail)
                .senderAdminId(senderAdminId)
                .channel(NotificationChannel.SITE)
                .businessType(businessType)
                .businessId(businessId)
                .title(title)
                .content(content)
                .sendStatus(NotificationSendStatus.SENT)
                .retryCount(0)
                .sentAt(LocalDateTime.now())
                .build());
    }

    /**
     * 邮件记录先以 PENDING 落库,再交给异步分发服务发送(成功 SENT / 失败 FAILED + 重试计数)。
     * 该方法在业务事务内执行时只做数据库写入,不产生网络 IO。
     */
    private void createEmailNotification(Long receiverUserId,
                                         String receiverEmail,
                                         Long senderAdminId,
                                         NotificationBusinessType businessType,
                                         Long businessId,
                                         String title,
                                         String content) {
        Notification notification = Notification.builder()
                .receiverUserId(receiverUserId)
                .receiverEmail(receiverEmail)
                .senderAdminId(senderAdminId)
                .channel(NotificationChannel.EMAIL)
                .businessType(businessType)
                .businessId(businessId)
                .title(title)
                .content(content)
                .sendStatus(NotificationSendStatus.PENDING)
                .retryCount(0)
                .build();
        notificationMapper.insert(notification);
        // 在事务提交后再异步发送:异步线程无法看到未提交的 PENDING 记录,提前发送会导致
        // 邮件发不出去(读不到行)或发送成功后状态无法回写、被重试任务重复发送。
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailDispatchService.send(notification.getNotificationId());
                }
            });
        } else {
            emailDispatchService.send(notification.getNotificationId());
        }
    }

    private String orderStatusLabel(OrderStatus status) {
        if (status == null) {
            return "未知状态";
        }
        return switch (status) {
            case PENDING_CONFIRM -> "待卖家确认";
            case AWAITING_DELIVERY -> "待发货";
            case DELIVERING -> "配送中";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
            case CLOSED -> "已关闭";
        };
    }

    private String buildMailSubject(String coreTitle) {
        return MAIL_SUBJECT_PREFIX + coreTitle;
    }
}
