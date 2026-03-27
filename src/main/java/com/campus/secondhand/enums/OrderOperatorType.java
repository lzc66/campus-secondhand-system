package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum OrderOperatorType {
    BUYER("buyer"),
    SELLER("seller"),
    ADMIN("admin"),
    SYSTEM("system");

    @EnumValue
    private final String value;

    OrderOperatorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
