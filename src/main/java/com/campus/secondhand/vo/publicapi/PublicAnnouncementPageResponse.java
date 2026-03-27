package com.campus.secondhand.vo.publicapi;

import java.util.List;

public record PublicAnnouncementPageResponse(
        long current,
        long size,
        long total,
        List<PublicAnnouncementResponse> records
) {
}
