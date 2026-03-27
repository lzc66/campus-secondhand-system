package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum OrderType {
    ONLINE_COD("online_cod"),
    OFFLINE_RECORD("offline_record");

    @EnumValue
    private final String value;

    OrderType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
