package com.campus.secondhand.vo.user;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long orderItemId,
        Long itemId,
        String itemTitleSnapshot,
        BigDecimal itemPriceSnapshot,
        Integer quantity,
        BigDecimal subtotalAmount
) {
}
