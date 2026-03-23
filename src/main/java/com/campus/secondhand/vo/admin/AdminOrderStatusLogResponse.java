package com.campus.secondhand.vo.admin;

import java.time.LocalDateTime;

public record AdminOrderStatusLogResponse(
        Long orderStatusLogId,
        String operatorType,
        Long operatorId,
        String fromStatus,
        String toStatus,
        String actionNote,
        LocalDateTime createdAt
) {
}
