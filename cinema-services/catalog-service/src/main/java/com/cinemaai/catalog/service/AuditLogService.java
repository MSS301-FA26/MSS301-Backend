package com.cinemaai.catalog.service;

import com.cinemaai.catalog.enums.AuditActionType;

public interface AuditLogService {
    void record(AuditActionType action, String targetType, Long targetId, String detail);
}
