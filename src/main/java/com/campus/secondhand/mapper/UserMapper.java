package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
            SELECT *
            FROM users
            WHERE student_no = #{studentNo} AND deleted_at IS NULL
            LIMIT 1
            """)
    User selectByStudentNo(@Param("studentNo") String studentNo);

    @Select("""
            SELECT *
            FROM users
            WHERE email = #{email} AND deleted_at IS NULL
            LIMIT 1
            """)
    User selectByEmail(@Param("email") String email);

    @Select("""
            SELECT DATE(created_at) AS d, COUNT(*) AS c
            FROM users
            WHERE deleted_at IS NULL
              AND created_at >= #{start} AND created_at < #{end}
            GROUP BY DATE(created_at)
            """)
    List<Map<String, Object>> selectUserGrowth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}