package com.campus.secondhand.service;

import com.campus.secondhand.dto.admin.SaveAnnouncementRequest;
import com.campus.secondhand.entity.Announcement;
import com.campus.secondhand.enums.AdminRoleCode;
import com.campus.secondhand.enums.AnnouncementPublishStatus;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.AnnouncementMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.impl.AdminAnnouncementServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminAnnouncementServiceTest {

    @Mock
    private AnnouncementMapper announcementMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AdminOperationLogMapper adminOperationLogMapper;

    @InjectMocks
    private AdminAnnouncementServiceImpl adminAnnouncementService;

    @Test
    void shouldCreatePublishedAnnouncementAndSendNotifications() {
        doAnswer(invocation -> {
            Announcement announcement = invocation.getArgument(0);
            announcement.setAnnouncementId(7L);
            return 1;
        }).when(announcementMapper).insert(any(Announcement.class));

        var response = adminAnnouncementService.create(
                new AdminPrincipal(1L, "admin1001", "Campus Admin", "admin@campus.local", AdminRoleCode.SUPER_ADMIN),
                new SaveAnnouncementRequest("System Notice", "Dorm delivery starts at 7pm.", true, "published", LocalDateTime.now().plusDays(2))
        );

        assertEquals(7L, response.announcementId());
        assertEquals(AnnouncementPublishStatus.PUBLISHED.getValue(), response.publishStatus());
        verify(notificationService).sendAnnouncementPublished(any(Announcement.class), any(Long.class));
        verify(adminOperationLogMapper).insert(any(com.campus.secondhand.entity.AdminOperationLog.class));
    }
}
