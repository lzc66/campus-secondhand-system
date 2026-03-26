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
@TableName("system_settings")
public class SystemSetting {

    @TableId(value = "setting_id", type = IdType.AUTO)
    private Long settingId;
    private String settingKey;
    private String settingValue;
    private String valueType;
    private Long updatedByAdminId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}