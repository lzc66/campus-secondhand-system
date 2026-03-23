package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum DeliveryType {
    DORM_DELIVERY("dorm_delivery"),
    SELF_PICKUP("self_pickup"),
    FACE_TO_FACE("face_to_face");

    @EnumValue
    private final String value;

    DeliveryType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
