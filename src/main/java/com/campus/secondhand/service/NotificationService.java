package com.campus.secondhand.service;

import com.campus.secondhand.entity.Announcement;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.ItemComment;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.entity.TradeOrder;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.OrderStatus;

public interface NotificationService {

    void sendRegistrationApproved(RegistrationApplication application, User user, Long adminId);

    void sendRegistrationRejected(RegistrationApplication application, Long adminId);

    void sendAnnouncementPublished(Announcement announcement, Long adminId);

    /**
     * 订单状态变更通知:站内信(SITE)立即写入,邮件(EMAIL)先落 PENDING 再异步发送。
     * senderAdminId 非空表示由管理员操作触发。
     */
    void sendOrderStatusChanged(TradeOrder order, Long receiverUserId, Long senderAdminId,
                                OrderStatus fromStatus, OrderStatus toStatus, String note);

    /**
     * 评论收到卖家回复通知(站内信 + 邮件)。
     */
    void sendCommentReplied(Item item, ItemComment reply, Long receiverUserId);
}
