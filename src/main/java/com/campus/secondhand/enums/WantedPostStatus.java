package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum WantedPostStatus {
    OPEN("open"),
    MATCHED("matched"),
    CLOSED("closed"),
    CANCELLED("cancelled");

    @EnumValue
    private final String value;

    WantedPostStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
