package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum NotificationBusinessType {
    REGISTER_REVIEW("register_review"),
    ORDER_UPDATE("order_update"),
    COMMENT_REPLY("comment_reply"),
    ANNOUNCEMENT("announcement"),
    SYSTEM("system");

    @EnumValue
    private final String value;

    NotificationBusinessType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}