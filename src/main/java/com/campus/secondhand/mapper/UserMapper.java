package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE student_no = #{studentNo} LIMIT 1")
    User selectByStudentNo(@Param("studentNo") String studentNo);

    @Select("SELECT * FROM users WHERE email = #{email} LIMIT 1")
    User selectByEmail(@Param("email") String email);
}