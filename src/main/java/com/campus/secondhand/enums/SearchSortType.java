package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum SearchSortType {
    DEFAULT("default"),
    LATEST("latest"),
    PRICE_ASC("price_asc"),
    PRICE_DESC("price_desc");

    @EnumValue
    private final String value;

    SearchSortType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
