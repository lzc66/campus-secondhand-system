package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.secondhand.enums.AdminAccountStatus;
import com.campus.secondhand.enums.AdminRoleCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("admins")
public class Admin {

    @TableId(value = "admin_id", type = IdType.AUTO)
    private Long adminId;
    private String adminNo;
    private String passwordHash;
    private String adminName;
    private String email;
    private AdminRoleCode roleCode;
    private AdminAccountStatus accountStatus;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
