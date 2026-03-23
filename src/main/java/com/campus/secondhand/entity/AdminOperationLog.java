package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("admin_operation_logs")
public class AdminOperationLog {

    @TableId(value = "admin_operation_log_id", type = IdType.AUTO)
    private Long adminOperationLogId;
    private Long adminId;
    private String targetType;
    private Long targetId;
    private String operationType;
    private String operationDetail;
    private String ipAddress;
    private LocalDateTime createdAt;
}
