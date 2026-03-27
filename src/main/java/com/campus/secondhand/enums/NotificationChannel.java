package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum NotificationChannel {
    SITE("site"),
    EMAIL("email");

    @EnumValue
    private final String value;

    NotificationChannel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}