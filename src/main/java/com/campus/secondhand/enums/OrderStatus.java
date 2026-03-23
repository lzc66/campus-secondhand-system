package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum OrderStatus {
    PENDING_CONFIRM("pending_confirm"),
    AWAITING_DELIVERY("awaiting_delivery"),
    DELIVERING("delivering"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    CLOSED("closed");

    @EnumValue
    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
