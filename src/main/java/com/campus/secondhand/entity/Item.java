package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.secondhand.enums.ItemConditionLevel;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.enums.ItemTradeMode;
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
@TableName("items")
public class Item {

    @TableId(value = "item_id", type = IdType.AUTO)
    private Long itemId;
    private Long sellerUserId;
    private Long categoryId;
    private String title;
    private String brand;
    private String model;
    private String description;
    private ItemConditionLevel conditionLevel;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private ItemTradeMode tradeMode;
    private Integer negotiable;
    private String contactPhone;
    private String contactQq;
    private String contactWechat;
    private String pickupAddress;
    private ItemStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime soldAt;
    private Integer viewCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}