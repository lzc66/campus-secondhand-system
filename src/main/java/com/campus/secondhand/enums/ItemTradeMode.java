package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ItemTradeMode {
    OFFLINE("offline"),
    ONLINE("online"),
    BOTH("both");

    @EnumValue
    private final String value;

    ItemTradeMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}