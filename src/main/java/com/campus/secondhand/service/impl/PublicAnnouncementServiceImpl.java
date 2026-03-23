package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.entity.Announcement;
import com.campus.secondhand.enums.AnnouncementPublishStatus;
import com.campus.secondhand.mapper.AnnouncementMapper;
import com.campus.secondhand.service.PublicAnnouncementService;
import com.campus.secondhand.vo.publicapi.PublicAnnouncementPageResponse;
import com.campus.secondhand.vo.publicapi.PublicAnnouncementResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class PublicAnnouncementServiceImpl implements PublicAnnouncementService {

    private final AnnouncementMapper announcementMapper;

    public PublicAnnouncementServiceImpl(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    @Override
    public PublicAnnouncementPageResponse listPublished(long page, long size) {
        Page<Announcement> queryPage = new Page<>(Math.max(page, 1), Math.max(size, 1));
        Page<Announcement> result = announcementMapper.selectPage(queryPage, new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getPublishStatus, AnnouncementPublishStatus.PUBLISHED)
                .and(q -> q.isNull(Announcement::getExpireAt).or().gt(Announcement::getExpireAt, LocalDateTime.now()))
                .orderByDesc(Announcement::getIsPinned)
                .orderByDesc(Announcement::getPublishedAt)
                .orderByDesc(Announcement::getCreatedAt));
        List<PublicAnnouncementResponse> records = result.getRecords().stream().map(this::toResponse).toList();
        return new PublicAnnouncementPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public PublicAnnouncementResponse detail(Long announcementId) {
        Announcement announcement = announcementMapper.selectById(announcementId);
        if (announcement == null || announcement.getPublishStatus() != AnnouncementPublishStatus.PUBLISHED
                || (announcement.getExpireAt() != null && !announcement.getExpireAt().isAfter(LocalDateTime.now()))) {
            throw new BusinessException(40490, HttpStatus.NOT_FOUND, "Announcement not found");
        }
        return toResponse(announcement);
    }

    private PublicAnnouncementResponse toResponse(Announcement announcement) {
        return new PublicAnnouncementResponse(
                announcement.getAnnouncementId(),
                announcement.getTitle(),
                announcement.getContent(),
                Objects.equals(announcement.getIsPinned(), 1),
                announcement.getPublishedAt(),
                announcement.getExpireAt()
        );
    }
}
