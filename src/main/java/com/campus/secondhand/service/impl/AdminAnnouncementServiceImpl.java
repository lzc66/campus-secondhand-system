package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.dto.admin.AnnouncementActionRequest;
import com.campus.secondhand.dto.admin.SaveAnnouncementRequest;
import com.campus.secondhand.entity.AdminOperationLog;
import com.campus.secondhand.entity.Announcement;
import com.campus.secondhand.enums.AnnouncementPublishStatus;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.AnnouncementMapper;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.AdminAnnouncementService;
import com.campus.secondhand.service.NotificationService;
import com.campus.secondhand.vo.admin.AdminAnnouncementDetailResponse;
import com.campus.secondhand.vo.admin.AdminAnnouncementPageResponse;
import com.campus.secondhand.vo.admin.AdminAnnouncementSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class AdminAnnouncementServiceImpl implements AdminAnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final NotificationService notificationService;
    private final AdminOperationLogMapper adminOperationLogMapper;

    public AdminAnnouncementServiceImpl(AnnouncementMapper announcementMapper,
                                        NotificationService notificationService,
                                        AdminOperationLogMapper adminOperationLogMapper) {
        this.announcementMapper = announcementMapper;
        this.notificationService = notificationService;
        this.adminOperationLogMapper = adminOperationLogMapper;
    }

    @Override
    @Transactional
    public AdminAnnouncementDetailResponse create(AdminPrincipal principal, SaveAnnouncementRequest request) {
        validateExpireAt(request.expireAt());
        AnnouncementPublishStatus publishStatus = parsePublishStatus(request.publishStatus(), AnnouncementPublishStatus.PUBLISHED);
        Announcement announcement = Announcement.builder()
                .publisherAdminId(principal.getAdminId())
                .title(request.title().trim())
                .content(request.content().trim())
                .isPinned(Boolean.TRUE.equals(request.pinned()) ? 1 : 0)
                .publishStatus(publishStatus)
                .publishedAt(publishStatus == AnnouncementPublishStatus.PUBLISHED ? LocalDateTime.now() : null)
                .expireAt(request.expireAt())
                .build();
        announcementMapper.insert(announcement);
        if (publishStatus == AnnouncementPublishStatus.PUBLISHED) {
            notificationService.sendAnnouncementPublished(announcement, principal.getAdminId());
        }
        logOperation(principal.getAdminId(), announcement.getAnnouncementId(), "create", publishStatus.getValue());
        return toDetail(announcement);
    }

    @Override
    @Transactional
    public AdminAnnouncementDetailResponse update(AdminPrincipal principal, Long announcementId, SaveAnnouncementRequest request) {
        validateExpireAt(request.expireAt());
        Announcement announcement = getRequiredAnnouncement(announcementId);
        AnnouncementPublishStatus beforeStatus = announcement.getPublishStatus();
        AnnouncementPublishStatus afterStatus = parsePublishStatus(request.publishStatus(), announcement.getPublishStatus());
        announcement.setTitle(request.title().trim());
        announcement.setContent(request.content().trim());
        announcement.setIsPinned(Boolean.TRUE.equals(request.pinned()) ? 1 : 0);
        announcement.setPublishStatus(afterStatus);
        announcement.setExpireAt(request.expireAt());
        if (beforeStatus != AnnouncementPublishStatus.PUBLISHED && afterStatus == AnnouncementPublishStatus.PUBLISHED) {
            announcement.setPublishedAt(LocalDateTime.now());
            notificationService.sendAnnouncementPublished(announcement, principal.getAdminId());
        }
        announcementMapper.updateById(announcement);
        logOperation(principal.getAdminId(), announcement.getAnnouncementId(), "update", afterStatus.getValue());
        return toDetail(announcement);
    }

    @Override
    public AdminAnnouncementPageResponse list(String publishStatus, long page, long size) {
        AnnouncementPublishStatus filter = StringUtils.hasText(publishStatus) ? parsePublishStatus(publishStatus, null) : null;
        Page<Announcement> queryPage = new Page<>(Math.max(page, 1), Math.max(size, 1));
        Page<Announcement> result = announcementMapper.selectPage(queryPage, new LambdaQueryWrapper<Announcement>()
                .eq(filter != null, Announcement::getPublishStatus, filter)
                .orderByDesc(Announcement::getIsPinned)
                .orderByDesc(Announcement::getPublishedAt)
                .orderByDesc(Announcement::getCreatedAt));
        List<AdminAnnouncementSummaryResponse> records = result.getRecords().stream().map(this::toSummary).toList();
        return new AdminAnnouncementPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public AdminAnnouncementDetailResponse detail(Long announcementId) {
        return toDetail(getRequiredAnnouncement(announcementId));
    }

    @Override
    @Transactional
    public AdminAnnouncementDetailResponse publish(AdminPrincipal principal, Long announcementId, AnnouncementActionRequest request) {
        Announcement announcement = getRequiredAnnouncement(announcementId);
        if (announcement.getPublishStatus() != AnnouncementPublishStatus.PUBLISHED) {
            announcement.setPublishStatus(AnnouncementPublishStatus.PUBLISHED);
            announcement.setPublishedAt(LocalDateTime.now());
            announcementMapper.updateById(announcement);
            notificationService.sendAnnouncementPublished(announcement, principal.getAdminId());
        }
        logOperation(principal.getAdminId(), announcement.getAnnouncementId(), "publish", request == null ? null : request.actionNote());
        return toDetail(announcement);
    }

    @Override
    @Transactional
    public AdminAnnouncementDetailResponse offline(AdminPrincipal principal, Long announcementId, AnnouncementActionRequest request) {
        Announcement announcement = getRequiredAnnouncement(announcementId);
        announcement.setPublishStatus(AnnouncementPublishStatus.OFFLINE);
        announcementMapper.updateById(announcement);
        logOperation(principal.getAdminId(), announcement.getAnnouncementId(), "offline", request == null ? null : request.actionNote());
        return toDetail(announcement);
    }

    private Announcement getRequiredAnnouncement(Long announcementId) {
        Announcement announcement = announcementMapper.selectById(announcementId);
        if (announcement == null) {
            throw new BusinessException(40490, HttpStatus.NOT_FOUND, "Announcement not found");
        }
        return announcement;
    }

    private AnnouncementPublishStatus parsePublishStatus(String value, AnnouncementPublishStatus defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "draft" -> AnnouncementPublishStatus.DRAFT;
            case "published" -> AnnouncementPublishStatus.PUBLISHED;
            case "offline" -> AnnouncementPublishStatus.OFFLINE;
            default -> throw new BusinessException(40090, HttpStatus.BAD_REQUEST, "publishStatus is invalid");
        };
    }

    private void validateExpireAt(LocalDateTime expireAt) {
        if (expireAt != null && expireAt.isBefore(LocalDateTime.now())) {
            throw new BusinessException(40091, HttpStatus.BAD_REQUEST, "expireAt must be in the future");
        }
    }

    private void logOperation(Long adminId, Long targetId, String action, String detail) {
        String operationDetail = "{\"action\":\"" + action + "\"}";
        adminOperationLogMapper.insert(AdminOperationLog.builder()
                .adminId(adminId)
                .targetType("announcement")
                .targetId(targetId)
                .operationType(action)
                .operationDetail(operationDetail)
                .ipAddress(null)
                .build());
    }
    private AdminAnnouncementSummaryResponse toSummary(Announcement announcement) {
        return new AdminAnnouncementSummaryResponse(
                announcement.getAnnouncementId(),
                announcement.getTitle(),
                Objects.equals(announcement.getIsPinned(), 1),
                announcement.getPublishStatus().getValue(),
                announcement.getPublishedAt(),
                announcement.getExpireAt(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }

    private AdminAnnouncementDetailResponse toDetail(Announcement announcement) {
        return new AdminAnnouncementDetailResponse(
                announcement.getAnnouncementId(),
                announcement.getPublisherAdminId(),
                announcement.getTitle(),
                announcement.getContent(),
                Objects.equals(announcement.getIsPinned(), 1),
                announcement.getPublishStatus().getValue(),
                announcement.getPublishedAt(),
                announcement.getExpireAt(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}
