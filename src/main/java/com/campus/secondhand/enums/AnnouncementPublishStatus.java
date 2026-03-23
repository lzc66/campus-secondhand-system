package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum AnnouncementPublishStatus {
    DRAFT("draft"),
    PUBLISHED("published"),
    OFFLINE("offline");

    @EnumValue
    private final String value;

    AnnouncementPublishStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
