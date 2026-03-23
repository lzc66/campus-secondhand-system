package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.user.UserLoginRequest;
import com.campus.secondhand.entity.LoginLog;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.mapper.LoginLogMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.JwtTokenProvider;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.UserAuthService;
import com.campus.secondhand.vo.user.UserLoginResponse;
import com.campus.secondhand.vo.user.UserProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserAuthServiceImpl implements UserAuthService {

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserAuthServiceImpl(UserMapper userMapper,
                               LoginLogMapper loginLogMapper,
                               PasswordEncoder passwordEncoder,
                               JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.loginLogMapper = loginLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public UserLoginResponse login(UserLoginRequest request, String ipAddress, String userAgent) {
        User user = userMapper.selectByStudentNo(request.studentNo());
        if (user == null) {
            saveLoginLog(null, request.studentNo(), "failure", "USER_NOT_FOUND", ipAddress, userAgent);
            throw new BusinessException(40120, HttpStatus.UNAUTHORIZED, "Student number or password is incorrect");
        }
        if (user.getAccountStatus() != UserAccountStatus.ACTIVE) {
            saveLoginLog(user.getUserId(), request.studentNo(), "failure", "USER_NOT_ACTIVE", ipAddress, userAgent);
            throw new BusinessException(40320, HttpStatus.FORBIDDEN, "User account is unavailable");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            saveLoginLog(user.getUserId(), request.studentNo(), "failure", "PASSWORD_NOT_MATCH", ipAddress, userAgent);
            throw new BusinessException(40121, HttpStatus.UNAUTHORIZED, "Student number or password is incorrect");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        saveLoginLog(user.getUserId(), request.studentNo(), "success", null, ipAddress, userAgent);

        String token = jwtTokenProvider.createUserToken(
                user.getUserId(),
                user.getStudentNo(),
                user.getAccountStatus().getValue()
        );
        return new UserLoginResponse(
                token,
                "Bearer",
                jwtTokenProvider.getExpirationSeconds(),
                toProfile(user)
        );
    }

    @Override
    public UserProfileResponse getCurrentUser(UserPrincipal principal) {
        User user = userMapper.selectById(principal.getUserId());
        if (user == null) {
            throw new BusinessException(40420, HttpStatus.NOT_FOUND, "User not found");
        }
        return toProfile(user);
    }

    private void saveLoginLog(Long accountId, String loginName, String loginResult, String failReason,
                              String ipAddress, String userAgent) {
        LoginLog loginLog = LoginLog.builder()
                .accountType("user")
                .accountId(accountId)
                .loginName(loginName)
                .loginResult(loginResult)
                .failReason(failReason)
                .captchaPassed(1)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        loginLogMapper.insert(loginLog);
    }

    private UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
                user.getUserId(),
                user.getStudentNo(),
                user.getRealName(),
                user.getEmail(),
                user.getPhone(),
                user.getCollegeName(),
                user.getMajorName(),
                user.getClassName(),
                user.getDormitoryAddress(),
                user.getAccountStatus().getValue()
        );
    }
}