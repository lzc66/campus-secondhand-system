package com.campus.secondhand.vo.user;

public record ItemImageResponse(
        Long fileId,
        String fileUrl,
        String originalName,
        Integer sortOrder,
        boolean cover
) {
}