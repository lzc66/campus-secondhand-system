package com.campus.secondhand.service;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.admin.ReviewRegistrationApplicationRequest;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.Gender;
import com.campus.secondhand.enums.RegistrationStatus;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.RegistrationApplicationMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.impl.RegistrationReviewServiceImpl;
import com.campus.secondhand.vo.admin.ReviewRegistrationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationReviewServiceTest {

    @Mock
    private RegistrationApplicationMapper registrationApplicationMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AdminOperationLogMapper adminOperationLogMapper;

    @InjectMocks
    private RegistrationReviewServiceImpl registrationReviewService;

    @Test
    void shouldApprovePendingApplication() {
        RegistrationApplication application = RegistrationApplication.builder()
                .applicationId(5L)
                .studentNo("20240001")
                .email("alice@campus.local")
                .passwordHash("hash")
                .realName("Alice")
                .gender(Gender.FEMALE)
                .collegeName("Engineering")
                .status(RegistrationStatus.PENDING)
                .studentCardFileId(8L)
                .build();
        when(registrationApplicationMapper.selectById(5L)).thenReturn(application);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(66L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        ReviewRegistrationResponse response = registrationReviewService.approve(
                5L,
                new ReviewRegistrationApplicationRequest("ok"),
                new AdminPrincipal(1L, "admin1001", "Campus Admin", "admin@campus.local", com.campus.secondhand.enums.AdminRoleCode.SUPER_ADMIN),
                "127.0.0.1"
        );

        assertEquals("approved", response.status());
        assertEquals(66L, response.userId());
        verify(notificationService).sendRegistrationApproved(any(RegistrationApplication.class), any(User.class), any(Long.class));
        verify(adminOperationLogMapper).insert(any(com.campus.secondhand.entity.AdminOperationLog.class));
    }

    @Test
    void shouldRejectReviewedApplication() {
        when(registrationApplicationMapper.selectById(5L)).thenReturn(RegistrationApplication.builder()
                .applicationId(5L)
                .status(RegistrationStatus.REJECTED)
                .studentCardFileId(8L)
                .build());

        assertThrows(BusinessException.class, () -> registrationReviewService.reject(
                5L,
                new ReviewRegistrationApplicationRequest("no"),
                new AdminPrincipal(1L, "admin1001", "Campus Admin", "admin@campus.local", com.campus.secondhand.enums.AdminRoleCode.SUPER_ADMIN),
                "127.0.0.1"
        ));
    }
}