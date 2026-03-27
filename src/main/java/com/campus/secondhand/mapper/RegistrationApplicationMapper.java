package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.RegistrationApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RegistrationApplicationMapper extends BaseMapper<RegistrationApplication> {

    @Select("""
            SELECT *
            FROM registration_applications
            WHERE student_no = #{studentNo} AND status = 'pending'
            LIMIT 1
            """)
    RegistrationApplication selectPendingByStudentNo(@Param("studentNo") String studentNo);
}