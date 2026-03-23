package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.AdminOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface AdminOperationLogMapper extends BaseMapper<AdminOperationLog> {

    @Select("""
            SELECT created_at
            FROM admin_operation_logs
            WHERE JSON_UNQUOTE(JSON_EXTRACT(operation_detail, '$.action')) = 'bootstrap'
            ORDER BY created_at DESC
            LIMIT 1
            """)
    LocalDateTime selectLatestBootstrapAt();
}
