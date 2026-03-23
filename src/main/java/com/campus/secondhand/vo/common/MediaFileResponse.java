package com.campus.secondhand.vo.common;

public record MediaFileResponse(
        Long fileId,
        String fileUrl,
        String originalName,
        Long fileSize,
        String mimeType
) {
}