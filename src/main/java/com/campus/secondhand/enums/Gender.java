package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum Gender {
    MALE("male"),
    FEMALE("female"),
    UNKNOWN("unknown");

    @EnumValue
    private final String value;

    Gender(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}