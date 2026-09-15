package com.campus.secondhand.vo.admin;

public record MediaFileDownloadResponse(
        Long fileId,
        String originalName,
        String mimeType,
        byte[] content
) {
}
