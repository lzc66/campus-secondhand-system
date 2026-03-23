package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ItemConditionLevel {
    NEW("new"),
    ALMOST_NEW("almost_new"),
    LIGHTLY_USED("lightly_used"),
    USED("used"),
    WELL_USED("well_used");

    @EnumValue
    private final String value;

    ItemConditionLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}