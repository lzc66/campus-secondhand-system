package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum UserBehaviorType {
    VIEW("view"),
    SEARCH("search"),
    COMMENT("comment"),
    PUBLISH("publish"),
    CONTACT("contact"),
    PURCHASE("purchase"),
    WANT_POST("want_post");

    @EnumValue
    private final String value;

    UserBehaviorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
