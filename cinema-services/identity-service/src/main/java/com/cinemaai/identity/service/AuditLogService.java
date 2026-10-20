package com.cinemaai.identity.service;

import com.cinemaai.identity.dto.response.PageResponse;
import com.cinemaai.identity.dto.response.audit.AuditLogResponse;
import com.cinemaai.identity.enums.AuditActionType;

public interface AuditLogService {

    void record(AuditActionType action, String targetType, Long targetId, String detail);

    void recordInternal(AuditActionType action, String targetType, Long targetId, String detail, Long actorId);

    PageResponse<AuditLogResponse> getLogs(int page, int size, String targetType);
}
