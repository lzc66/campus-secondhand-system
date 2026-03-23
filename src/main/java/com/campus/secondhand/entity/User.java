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
@TableName("users")
public class User {

    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;
    private Long applicationId;
    private String studentNo;
    private String email;
    private String passwordHash;
    private String realName;
    private String gender;
    private String phone;
    private String qqNo;
    private String wechatNo;
    private Long avatarFileId;
    private String collegeName;
    private String majorName;
    private String className;
    private String dormitoryAddress;
    private String accountStatus;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
