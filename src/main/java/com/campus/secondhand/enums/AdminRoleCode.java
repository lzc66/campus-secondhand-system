package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum AdminRoleCode {
    SUPER_ADMIN("super_admin"),
    AUDITOR("auditor"),
    OPERATOR("operator");

    @EnumValue
    private final String value;

    AdminRoleCode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
