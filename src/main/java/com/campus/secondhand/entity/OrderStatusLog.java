package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.secondhand.enums.OrderOperatorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("order_status_logs")
public class OrderStatusLog {

    @TableId(value = "order_status_log_id", type = IdType.AUTO)
    private Long orderStatusLogId;
    private Long orderId;
    private OrderOperatorType operatorType;
    private Long operatorId;
    private String fromStatus;
    private String toStatus;
    private String actionNote;
    private LocalDateTime createdAt;
}
