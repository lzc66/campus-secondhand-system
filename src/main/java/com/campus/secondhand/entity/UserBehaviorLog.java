package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.secondhand.enums.UserBehaviorType;
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
@TableName("user_behavior_logs")
public class UserBehaviorLog {

    @TableId(value = "behavior_log_id", type = IdType.AUTO)
    private Long behaviorLogId;
    private Long userId;
    private Long itemId;
    private Long wantedPostId;
    private UserBehaviorType behaviorType;
    private String sourcePage;
    private String searchKeyword;
    private BigDecimal weight;
    private String extraJson;
    private LocalDateTime occurredAt;
}
