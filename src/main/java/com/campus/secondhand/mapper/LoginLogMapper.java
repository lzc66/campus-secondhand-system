package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {

    @Select("""
            SELECT COUNT(*)
            FROM login_logs
            WHERE account_type = #{accountType}
              AND login_name = #{loginName}
              AND login_result = 'failure'
              AND created_at >= #{since}
            """)
    long countRecentFailures(@Param("accountType") String accountType,
                             @Param("loginName") String loginName,
                             @Param("since") LocalDateTime since);
}
