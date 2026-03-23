package com.campus.secondhand.vo.user;

public record UserProfileResponse(
        Long userId,
        String studentNo,
        String realName,
        String email,
        String phone,
        String qqNo,
        String wechatNo,
        Long avatarFileId,
        String avatarUrl,
        String collegeName,
        String majorName,
        String className,
        String dormitoryAddress,
        String accountStatus
) {
}