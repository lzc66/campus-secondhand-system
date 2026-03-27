package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.secondhand.enums.NotificationBusinessType;
import com.campus.secondhand.enums.NotificationChannel;
import com.campus.secondhand.enums.NotificationSendStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("notifications")
public class Notification {

    @TableId(value = "notification_id", type = IdType.AUTO)
    private Long notificationId;
    private Long receiverUserId;
    private String receiverEmail;
    private Long senderAdminId;
    private NotificationChannel channel;
    private NotificationBusinessType businessType;
    private Long businessId;
    private String title;
    private String content;
    private NotificationSendStatus sendStatus;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}