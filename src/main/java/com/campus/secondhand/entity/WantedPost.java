package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.secondhand.enums.WantedPostStatus;
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
@TableName("wanted_posts")
public class WantedPost {

    @TableId(value = "wanted_post_id", type = IdType.AUTO)
    private Long wantedPostId;
    private Long requesterUserId;
    private Long categoryId;
    private String title;
    private String brand;
    private String model;
    private String description;
    private BigDecimal expectedPriceMin;
    private BigDecimal expectedPriceMax;
    private String contactPhone;
    private String contactQq;
    private String contactWechat;
    private WantedPostStatus status;
    private LocalDateTime expiresAt;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
