package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.user.UserLoginRequest;
import com.campus.secondhand.entity.LoginLog;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.mapper.LoginLogMapper;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.JwtTokenProvider;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.AdminDemoModeService;
import com.campus.secondhand.service.LoginCaptchaService;
import com.campus.secondhand.service.UserAuthService;
import com.campus.secondhand.vo.user.UserCaptchaResponse;
import com.campus.secondhand.vo.user.UserLoginResponse;
import com.campus.secondhand.vo.user.UserProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserAuthServiceImpl implements UserAuthService {

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final int FAILURE_WINDOW_MINUTES = 15;
    private static final List<String> DEMO_USER_STUDENT_NOS = List.of("20250001", "20250002", "20250003", "20250004");

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MediaFileMapper mediaFileMapper;
    private final LoginCaptchaService loginCaptchaService;
    private final AdminDemoModeService adminDemoModeService;

    public UserAuthServiceImpl(UserMapper userMapper,
                               LoginLogMapper loginLogMapper,
                               PasswordEncoder passwordEncoder,
                               JwtTokenProvider jwtTokenProvider,
                               MediaFileMapper mediaFileMapper,
                               LoginCaptchaService loginCaptchaService,
                               AdminDemoModeService adminDemoModeService) {
        this.userMapper = userMapper;
        this.loginLogMapper = loginLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.mediaFileMapper = mediaFileMapper;
        this.loginCaptchaService = loginCaptchaService;
        this.adminDemoModeService = adminDemoModeService;
    }

    @Override
    public UserCaptchaResponse getLoginCaptcha() {
        return loginCaptchaService.generateUserLoginCaptcha();
    }

    @Override
    public UserLoginResponse login(UserLoginRequest request, String ipAddress, String userAgent) {
        if (!loginCaptchaService.verifyUserLoginCaptcha(request.captchaKey(), request.captcha())) {
            saveLoginLog(null, request.studentNo(), "failure", "CAPTCHA_INVALID", 0, ipAddress, userAgent);
            throw new BusinessException(40122, HttpStatus.UNAUTHORIZED, "Captcha is invalid or expired");
        }

        User user = userMapper.selectByStudentNo(request.studentNo());
        if (user == null) {
            saveLoginLog(null, request.studentNo(), "failure", "USER_NOT_FOUND", 1, ipAddress, userAgent);
            throw new BusinessException(40120, HttpStatus.UNAUTHORIZED, "Student number or password is incorrect");
        }
        if (user.getAccountStatus() != UserAccountStatus.ACTIVE) {
            saveLoginLog(user.getUserId(), request.studentNo(), "failure", "USER_NOT_ACTIVE", 1, ipAddress, userAgent);
            throw new BusinessException(40320, HttpStatus.FORBIDDEN, "User account is unavailable");
        }
        // 演示账号:演示模式关闭后禁止登录,防止种子数据里的弱口令账号在生产环境被利用
        if (DEMO_USER_STUDENT_NOS.contains(request.studentNo()) && !adminDemoModeService.isDemoModeEnabled()) {
            saveLoginLog(user.getUserId(), request.studentNo(), "failure", "DEMO_MODE_DISABLED", 1, ipAddress, userAgent);
            throw new BusinessException(40322, HttpStatus.FORBIDDEN, "Demo accounts are only available when demo mode is enabled");
        }
        // 失败锁定:时间窗内失败次数达到上限即锁定账号,防止在线暴力破解
        long recentFailures = loginLogMapper.countRecentFailures("user", request.studentNo(),
                LocalDateTime.now().minusMinutes(FAILURE_WINDOW_MINUTES));
        if (recentFailures >= MAX_LOGIN_FAILURES) {
            user.setAccountStatus(UserAccountStatus.LOCKED);
            userMapper.updateById(user);
            saveLoginLog(user.getUserId(), request.studentNo(), "failure", "LOCKED_AFTER_FAILURES", 1, ipAddress, userAgent);
            throw new BusinessException(40321, HttpStatus.FORBIDDEN,
                    "Account locked due to repeated failed logins, please contact the administrator");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            saveLoginLog(user.getUserId(), request.studentNo(), "failure", "PASSWORD_NOT_MATCH", 1, ipAddress, userAgent);
            throw new BusinessException(40121, HttpStatus.UNAUTHORIZED, "Student number or password is incorrect");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        saveLoginLog(user.getUserId(), request.studentNo(), "success", null, 1, ipAddress, userAgent);

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

    private void saveLoginLog(Long accountId, String loginName, String loginResult, String failReason, int captchaPassed,
                              String ipAddress, String userAgent) {
        LoginLog loginLog = LoginLog.builder()
                .accountType("user")
                .accountId(accountId)
                // 客户端可控字段一律截断到列长度,防止超长 User-Agent 等在签发 token 前把登录流程打穿
                .loginName(truncate(loginName, 64))
                .loginResult(loginResult)
                .failReason(truncate(failReason, 64))
                .captchaPassed(captchaPassed)
                .ipAddress(truncate(ipAddress, 45))
                .userAgent(truncate(userAgent, 255))
                .build();
        loginLogMapper.insert(loginLog);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private UserProfileResponse toProfile(User user) {
        MediaFile avatar = user.getAvatarFileId() == null ? null : mediaFileMapper.selectById(user.getAvatarFileId());
        return new UserProfileResponse(
                user.getUserId(),
                user.getStudentNo(),
                user.getRealName(),
                user.getEmail(),
                user.getPhone(),
                user.getQqNo(),
                user.getWechatNo(),
                user.getAvatarFileId(),
                avatar == null ? null : avatar.getFileUrl(),
                user.getCollegeName(),
                user.getMajorName(),
                user.getClassName(),
                user.getDormitoryAddress(),
                user.getAccountStatus().getValue()
        );
    }
}