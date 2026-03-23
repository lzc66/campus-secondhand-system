package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ItemCommentStatus {
    VISIBLE("visible"),
    HIDDEN("hidden"),
    DELETED("deleted");

    @EnumValue
    private final String value;

    ItemCommentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}