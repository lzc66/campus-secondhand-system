package com.campus.secondhand.service;

import com.campus.secondhand.dto.admin.UpdateUserStatusRequest;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.impl.AdminUserManagementServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserManagementServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private MediaFileMapper mediaFileMapper;
    @Mock
    private ItemMapper itemMapper;
    @Mock
    private AdminOperationLogMapper adminOperationLogMapper;

    @InjectMocks
    private AdminUserManagementServiceImpl adminUserManagementService;

    @Test
    void shouldUpdateUserStatusAndWriteAuditLog() {
        User user = User.builder()
                .userId(1L)
                .studentNo("20240001")
                .realName("Alice")
                .email("alice@campus.local")
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(itemMapper.selectCount(any())).thenReturn(2L);

        var response = adminUserManagementService.updateStatus(
                new AdminPrincipal(9001L, "admin1001", "Campus Admin", "admin@campus.local", com.campus.secondhand.enums.AdminRoleCode.SUPER_ADMIN),
                1L,
                new UpdateUserStatusRequest("disabled", "manual review")
        );

        assertEquals("disabled", response.accountStatus());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(UserAccountStatus.DISABLED, userCaptor.getValue().getAccountStatus());
        verify(adminOperationLogMapper).insert(any(com.campus.secondhand.entity.AdminOperationLog.class));
    }
}
