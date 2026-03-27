package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum CancelledByType {
    BUYER("buyer"),
    SELLER("seller"),
    ADMIN("admin"),
    SYSTEM("system");

    @EnumValue
    private final String value;

    CancelledByType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
