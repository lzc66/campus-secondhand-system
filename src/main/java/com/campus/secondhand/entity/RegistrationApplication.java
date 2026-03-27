package com.campus.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.secondhand.enums.Gender;
import com.campus.secondhand.enums.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("registration_applications")
public class RegistrationApplication {

    @TableId(value = "application_id", type = IdType.AUTO)
    private Long applicationId;
    private String applicationNo;
    private String studentNo;
    private String realName;
    private Gender gender;
    private String email;
    private String phone;
    private String passwordHash;
    private String collegeName;
    private String majorName;
    private String className;
    private Long studentCardFileId;
    private RegistrationStatus status;
    private Long reviewerAdminId;
    private String reviewRemark;
    private LocalDateTime reviewedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
}