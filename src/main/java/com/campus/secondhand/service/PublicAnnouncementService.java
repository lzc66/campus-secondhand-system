package com.campus.secondhand.service;

import com.campus.secondhand.vo.publicapi.PublicAnnouncementPageResponse;
import com.campus.secondhand.vo.publicapi.PublicAnnouncementResponse;

public interface PublicAnnouncementService {

    PublicAnnouncementPageResponse listPublished(long page, long size);

    PublicAnnouncementResponse detail(Long announcementId);
}
