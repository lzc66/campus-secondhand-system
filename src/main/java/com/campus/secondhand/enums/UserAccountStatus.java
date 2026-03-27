package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum UserAccountStatus {
    ACTIVE("active"),
    DISABLED("disabled"),
    LOCKED("locked");

    @EnumValue
    private final String value;

    UserAccountStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}