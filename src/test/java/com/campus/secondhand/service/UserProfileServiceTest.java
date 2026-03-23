package com.campus.secondhand.service;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.user.ChangePasswordRequest;
import com.campus.secondhand.dto.user.UpdateUserProfileRequest;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.impl.UserProfileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private MediaFileMapper mediaFileMapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    @Test
    void shouldUpdateProfile() {
        User user = User.builder()
                .userId(11L)
                .studentNo("20240001")
                .realName("Alice")
                .email("alice@campus.local")
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
        when(userMapper.selectById(11L)).thenReturn(user);

        var response = userProfileService.updateProfile(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", UserAccountStatus.ACTIVE),
                new UpdateUserProfileRequest("Alice Chen", "alice2@campus.local", "13800000000", "123", "wx1", "Engineering", "SE", "Class 1", "Dorm 101")
        );

        assertEquals("Alice Chen", response.realName());
        assertEquals("alice2@campus.local", response.email());
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void shouldRejectWrongCurrentPassword() {
        User user = User.builder()
                .userId(11L)
                .studentNo("20240001")
                .passwordHash("hash")
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
        when(userMapper.selectById(11L)).thenReturn(user);
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThrows(BusinessException.class, () -> userProfileService.changePassword(
                new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", UserAccountStatus.ACTIVE),
                new ChangePasswordRequest("bad", "123456")
        ));
    }
}