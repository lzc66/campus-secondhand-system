package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.secondhand.enums.RecommendationReasonCode;
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
@TableName("user_recommendations")
public class UserRecommendation {

    @TableId(value = "recommendation_id", type = IdType.AUTO)
    private Long recommendationId;
    private Long userId;
    private Long itemId;
    private BigDecimal recommendScore;
    private RecommendationReasonCode reasonCode;
    private Integer isClicked;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;
}
