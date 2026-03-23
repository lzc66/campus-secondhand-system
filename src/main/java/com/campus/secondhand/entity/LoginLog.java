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
@TableName("login_logs")
public class LoginLog {

    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;
    private String accountType;
    private Long accountId;
    private String loginName;
    private String loginResult;
    private String failReason;
    private Integer captchaPassed;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime loggedAt;
}
