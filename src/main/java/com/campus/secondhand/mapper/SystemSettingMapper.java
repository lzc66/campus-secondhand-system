package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.SystemSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemSettingMapper extends BaseMapper<SystemSetting> {

    @Select("SELECT * FROM system_settings WHERE setting_key = #{settingKey} LIMIT 1")
    SystemSetting selectBySettingKey(@Param("settingKey") String settingKey);
}