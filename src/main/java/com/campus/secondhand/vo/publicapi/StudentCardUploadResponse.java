package com.campus.secondhand.vo.publicapi;

public record StudentCardUploadResponse(
        Long fileId,
        String fileUrl,
        String originalName,
        Long fileSize
) {
}