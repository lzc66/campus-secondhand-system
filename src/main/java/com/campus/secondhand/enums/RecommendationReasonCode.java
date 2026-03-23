package com.campus.secondhand.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum RecommendationReasonCode {
    SIMILAR_CATEGORY("similar_category"),
    RECENT_VIEW("recent_view"),
    HOT_SALE("hot_sale"),
    KEYWORD_MATCH("keyword_match"),
    COLLABORATIVE_FILTERING("collaborative_filtering"),
    MANUAL("manual");

    @EnumValue
    private final String value;

    RecommendationReasonCode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
