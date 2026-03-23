package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.entity.Announcement;
import com.campus.secondhand.enums.AnnouncementPublishStatus;
import com.campus.secondhand.mapper.AnnouncementMapper;
import com.campus.secondhand.service.impl.PublicAnnouncementServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicAnnouncementServiceTest {

    @Mock
    private AnnouncementMapper announcementMapper;

    @InjectMocks
    private PublicAnnouncementServiceImpl publicAnnouncementService;

    @Test
    void shouldListPublishedAnnouncements() {
        Page<Announcement> pageResult = new Page<>(1, 10, 1);
        pageResult.setRecords(List.of(Announcement.builder()
                .announcementId(3L)
                .title("System Notice")
                .content("Dorm delivery starts at 7pm.")
                .isPinned(1)
                .publishStatus(AnnouncementPublishStatus.PUBLISHED)
                .publishedAt(LocalDateTime.now())
                .build()));
        when(announcementMapper.selectPage(any(), any())).thenReturn(pageResult);

        var response = publicAnnouncementService.listPublished(1, 10);

        assertEquals(1, response.total());
        assertEquals("System Notice", response.records().get(0).title());
    }
}
