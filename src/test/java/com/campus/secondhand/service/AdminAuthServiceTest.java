package com.campus.secondhand.service;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.admin.AdminLoginRequest;
import com.campus.secondhand.entity.Admin;
import com.campus.secondhand.entity.LoginLog;
import com.campus.secondhand.enums.AdminAccountStatus;
import com.campus.secondhand.enums.AdminRoleCode;
import com.campus.secondhand.mapper.AdminMapper;
import com.campus.secondhand.mapper.LoginLogMapper;
import com.campus.secondhand.security.JwtTokenProvider;
import com.campus.secondhand.service.impl.AdminAuthServiceImpl;
import com.campus.secondhand.vo.admin.AdminLoginResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private AdminMapper adminMapper;
    @Mock
    private LoginLogMapper loginLogMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AdminAuthServiceImpl adminAuthService;

    @Test
    void shouldLoginWhenPasswordCorrect() {
        Admin admin = Admin.builder()
                .adminId(1L)
                .adminNo("admin1001")
                .adminName("Campus Admin")
                .email("admin@campus.local")
                .passwordHash("hash")
                .roleCode(AdminRoleCode.SUPER_ADMIN)
                .accountStatus(AdminAccountStatus.ACTIVE)
                .build();
        when(adminMapper.selectByAdminNo("admin1001")).thenReturn(admin);
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(jwtTokenProvider.createToken(1L, "admin1001", "super_admin")).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationSeconds()).thenReturn(86400L);

        AdminLoginResponse response = adminAuthService.login(new AdminLoginRequest("admin1001", "123456"), "127.0.0.1", "JUnit");

        assertEquals("jwt-token", response.token());
        assertEquals("admin1001", response.adminProfile().adminNo());
        verify(adminMapper).updateById(any(Admin.class));
        verify(loginLogMapper).insert(any(LoginLog.class));
    }

    @Test
    void shouldFailWhenPasswordIncorrect() {
        Admin admin = Admin.builder()
                .adminId(1L)
                .adminNo("admin1001")
                .passwordHash("hash")
                .roleCode(AdminRoleCode.SUPER_ADMIN)
                .accountStatus(AdminAccountStatus.ACTIVE)
                .build();
        when(adminMapper.selectByAdminNo("admin1001")).thenReturn(admin);
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> adminAuthService.login(new AdminLoginRequest("admin1001", "bad"), "127.0.0.1", "JUnit"));

        verify(adminMapper, never()).updateById(any(Admin.class));
        verify(loginLogMapper).insert(any(LoginLog.class));
    }

    @Test
    void shouldFailWhenAdminDisabled() {
        Admin admin = Admin.builder()
                .adminId(1L)
                .adminNo("admin1001")
                .passwordHash("hash")
                .roleCode(AdminRoleCode.SUPER_ADMIN)
                .accountStatus(AdminAccountStatus.DISABLED)
                .build();
        when(adminMapper.selectByAdminNo("admin1001")).thenReturn(admin);

        assertThrows(BusinessException.class,
                () -> adminAuthService.login(new AdminLoginRequest("admin1001", "123456"), "127.0.0.1", "JUnit"));

        verify(loginLogMapper).insert(any(LoginLog.class));
        verify(adminMapper, never()).updateById(any(Admin.class));
    }
}
