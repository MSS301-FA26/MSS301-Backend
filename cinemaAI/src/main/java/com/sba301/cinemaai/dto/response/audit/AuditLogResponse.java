package com.sba301.cinemaai.dto.response.audit;

import com.sba301.cinemaai.enums.AuditActionType;
import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String actorEmail,
        String actorName,
        AuditActionType action,
        String targetType,
        Long targetId,
        String detail,
        LocalDateTime createdAt
) {
}
