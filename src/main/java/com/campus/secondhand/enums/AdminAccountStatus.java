package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum AdminAccountStatus {
    ACTIVE("active"),
    DISABLED("disabled");

    @EnumValue
    private final String value;

    AdminAccountStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
