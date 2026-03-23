package com.campus.secondhand.vo.user;

public record OrderPartyResponse(
        Long userId,
        String studentNo,
        String realName,
        String phone
) {
}
