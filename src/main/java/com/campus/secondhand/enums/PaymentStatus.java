package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum PaymentStatus {
    UNPAID("unpaid"),
    PAID("paid"),
    CANCELLED("cancelled");

    @EnumValue
    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
