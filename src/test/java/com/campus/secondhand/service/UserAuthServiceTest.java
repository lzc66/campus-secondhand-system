package com.campus.secondhand.service;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.user.UserLoginRequest;
import com.campus.secondhand.entity.LoginLog;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.Gender;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.mapper.LoginLogMapper;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.JwtTokenProvider;
import com.campus.secondhand.service.impl.UserAuthServiceImpl;
import com.campus.secondhand.vo.user.UserLoginResponse;
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
class UserAuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private LoginLogMapper loginLogMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private MediaFileMapper mediaFileMapper;

    @InjectMocks
    private UserAuthServiceImpl userAuthService;

    @Test
    void shouldLoginWhenPasswordCorrect() {
        User user = User.builder()
                .userId(11L)
                .studentNo("20240001")
                .realName("Alice")
                .email("alice@campus.local")
                .gender(Gender.FEMALE)
                .passwordHash("hash")
                .collegeName("Engineering")
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
        when(userMapper.selectByStudentNo("20240001")).thenReturn(user);
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(jwtTokenProvider.createUserToken(11L, "20240001", "active")).thenReturn("user-token");
        when(jwtTokenProvider.getExpirationSeconds()).thenReturn(86400L);

        UserLoginResponse response = userAuthService.login(new UserLoginRequest("20240001", "123456", null, null), "127.0.0.1", "JUnit");

        assertEquals("user-token", response.token());
        verify(userMapper).updateById(any(User.class));
        verify(loginLogMapper).insert(any(LoginLog.class));
    }

    @Test
    void shouldFailWhenUserDisabled() {
        User user = User.builder()
                .userId(11L)
                .studentNo("20240001")
                .passwordHash("hash")
                .accountStatus(UserAccountStatus.DISABLED)
                .build();
        when(userMapper.selectByStudentNo("20240001")).thenReturn(user);

        assertThrows(BusinessException.class,
                () -> userAuthService.login(new UserLoginRequest("20240001", "123456", null, null), "127.0.0.1", "JUnit"));

        verify(loginLogMapper).insert(any(LoginLog.class));
        verify(userMapper, never()).updateById(any(User.class));
    }
}