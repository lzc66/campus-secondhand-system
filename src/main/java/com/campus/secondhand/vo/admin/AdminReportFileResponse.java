package com.campus.secondhand.vo.admin;

public record AdminReportFileResponse(
        String fileName,
        String contentType,
        byte[] content
) {
}