package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.admin.AdminLoginRequest;
import com.campus.secondhand.entity.Admin;
import com.campus.secondhand.entity.LoginLog;
import com.campus.secondhand.enums.AdminAccountStatus;
import com.campus.secondhand.mapper.AdminMapper;
import com.campus.secondhand.mapper.LoginLogMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.security.JwtTokenProvider;
import com.campus.secondhand.service.AdminAuthService;
import com.campus.secondhand.vo.admin.AdminLoginResponse;
import com.campus.secondhand.vo.admin.AdminProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminMapper adminMapper;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminAuthServiceImpl(AdminMapper adminMapper,
                                LoginLogMapper loginLogMapper,
                                PasswordEncoder passwordEncoder,
                                JwtTokenProvider jwtTokenProvider) {
        this.adminMapper = adminMapper;
        this.loginLogMapper = loginLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public AdminLoginResponse login(AdminLoginRequest request, String ipAddress, String userAgent) {
        Admin admin = adminMapper.selectByAdminNo(request.adminNo());
        if (admin == null) {
            saveLoginLog(null, request.adminNo(), "failure", "ADMIN_NOT_FOUND", ipAddress, userAgent);
            throw new BusinessException(40101, HttpStatus.UNAUTHORIZED, "Admin account or password is incorrect");
        }
        if (admin.getAccountStatus() != AdminAccountStatus.ACTIVE) {
            saveLoginLog(admin.getAdminId(), request.adminNo(), "failure", "ADMIN_DISABLED", ipAddress, userAgent);
            throw new BusinessException(40301, HttpStatus.FORBIDDEN, "Admin account is disabled");
        }
        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            saveLoginLog(admin.getAdminId(), request.adminNo(), "failure", "PASSWORD_NOT_MATCH", ipAddress, userAgent);
            throw new BusinessException(40102, HttpStatus.UNAUTHORIZED, "Admin account or password is incorrect");
        }

        admin.setLastLoginAt(LocalDateTime.now());
        adminMapper.updateById(admin);
        saveLoginLog(admin.getAdminId(), request.adminNo(), "success", null, ipAddress, userAgent);

        String token = jwtTokenProvider.createToken(admin.getAdminId(), admin.getAdminNo(), admin.getRoleCode().getValue());
        return new AdminLoginResponse(
                token,
                "Bearer",
                jwtTokenProvider.getExpirationSeconds(),
                toProfile(admin)
        );
    }

    @Override
    public AdminProfileResponse getCurrentAdmin(AdminPrincipal principal) {
        Admin admin = adminMapper.selectById(principal.getAdminId());
        if (admin == null) {
            throw new BusinessException(40401, HttpStatus.NOT_FOUND, "Admin not found");
        }
        return toProfile(admin);
    }

    private void saveLoginLog(Long accountId, String loginName, String loginResult, String failReason,
                              String ipAddress, String userAgent) {
        LoginLog loginLog = LoginLog.builder()
                .accountType("admin")
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

    private AdminProfileResponse toProfile(Admin admin) {
        return new AdminProfileResponse(
                admin.getAdminId(),
                admin.getAdminNo(),
                admin.getAdminName(),
                admin.getEmail(),
                admin.getRoleCode().getValue(),
                admin.getAccountStatus().getValue()
        );
    }
}
