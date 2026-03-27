package com.campus.secondhand.service;

import com.campus.secondhand.dto.admin.AnnouncementActionRequest;
import com.campus.secondhand.dto.admin.SaveAnnouncementRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.vo.admin.AdminAnnouncementDetailResponse;
import com.campus.secondhand.vo.admin.AdminAnnouncementPageResponse;

public interface AdminAnnouncementService {

    AdminAnnouncementDetailResponse create(AdminPrincipal principal, SaveAnnouncementRequest request);

    AdminAnnouncementDetailResponse update(AdminPrincipal principal, Long announcementId, SaveAnnouncementRequest request);

    AdminAnnouncementPageResponse list(String publishStatus, long page, long size);

    AdminAnnouncementDetailResponse detail(Long announcementId);

    AdminAnnouncementDetailResponse publish(AdminPrincipal principal, Long announcementId, AnnouncementActionRequest request);

    AdminAnnouncementDetailResponse offline(AdminPrincipal principal, Long announcementId, AnnouncementActionRequest request);
}
