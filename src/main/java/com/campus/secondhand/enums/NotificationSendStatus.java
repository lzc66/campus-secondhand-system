package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum NotificationSendStatus {
    PENDING("pending"),
    SENT("sent"),
    FAILED("failed");

    @EnumValue
    private final String value;

    NotificationSendStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}