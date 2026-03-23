package com.campus.secondhand.service;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.publicapi.RegistrationApplicationSubmitRequest;
import com.campus.secondhand.entity.MediaFile;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.mapper.RegistrationApplicationMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.service.impl.PublicRegistrationServiceImpl;
import com.campus.secondhand.vo.publicapi.RegistrationApplicationSubmitResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicRegistrationServiceTest {

    @Mock
    private RegistrationApplicationMapper registrationApplicationMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PublicRegistrationServiceImpl publicRegistrationService;

    @Test
    void shouldSubmitRegistrationApplication() {
        RegistrationApplicationSubmitRequest request = new RegistrationApplicationSubmitRequest(
                "20240001", "Alice", "female", "alice@campus.local", "13800000000", "123456",
                "Engineering", "Software", "Class 1", 9L
        );
        when(fileStorageService.getRequiredFile(9L)).thenReturn(MediaFile.builder().fileId(9L).fileCategory("image").build());
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        doAnswer(invocation -> {
            RegistrationApplication application = invocation.getArgument(0);
            application.setApplicationId(100L);
            return 1;
        }).when(registrationApplicationMapper).insert(any(RegistrationApplication.class));

        RegistrationApplicationSubmitResponse response = publicRegistrationService.submit(request);

        assertEquals(100L, response.applicationId());
        assertEquals("pending", response.status());
        ArgumentCaptor<RegistrationApplication> captor = ArgumentCaptor.forClass(RegistrationApplication.class);
        verify(registrationApplicationMapper).insert(captor.capture());
        assertEquals("encoded", captor.getValue().getPasswordHash());
    }

    @Test
    void shouldRejectWhenPendingApplicationExists() {
        when(registrationApplicationMapper.selectPendingByStudentNo("20240001")).thenReturn(RegistrationApplication.builder().build());

        assertThrows(BusinessException.class, () -> publicRegistrationService.submit(new RegistrationApplicationSubmitRequest(
                "20240001", "Alice", "female", "alice@campus.local", null, "123456",
                "Engineering", null, null, 9L
        )));

        verify(registrationApplicationMapper, never()).insert(any(RegistrationApplication.class));
    }
}