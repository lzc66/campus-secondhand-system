package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("order_items")
public class OrderItem {

    @TableId(value = "order_item_id", type = IdType.AUTO)
    private Long orderItemId;
    private Long orderId;
    private Long itemId;
    private String itemTitleSnapshot;
    private BigDecimal itemPriceSnapshot;
    private Integer quantity;
    private BigDecimal subtotalAmount;
    private LocalDateTime createdAt;
}
