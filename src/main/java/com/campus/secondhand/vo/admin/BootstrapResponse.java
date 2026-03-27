package com.campus.secondhand.vo.admin;

public record BootstrapResponse(
        int createdCount,
        int updatedCount,
        int skippedCount
) {
}
