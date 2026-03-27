package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ItemStatus {
    DRAFT("draft"),
    ON_SALE("on_sale"),
    RESERVED("reserved"),
    SOLD("sold"),
    OFF_SHELF("off_shelf"),
    DELETED("deleted");

    @EnumValue
    private final String value;

    ItemStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}