package com.campus.secondhand.service;

public interface EmailDispatchService {

    /**
     * 异步发送指定的邮件通知记录(notificationId 对应的 channel 必须为 EMAIL)。
     * 成功置 SENT,失败置 FAILED 并累加重试次数,由 EmailRetryJob 兜底重投。
     */
    void send(Long notificationId);
}
