package com.cinemaai.catalog.service.impl;

import com.cinemaai.catalog.enums.AuditActionType;
import com.cinemaai.catalog.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {
    public void record(AuditActionType action, String targetType, Long targetId, String detail) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Catalog audit actor={} action={} resource={} id={}",
                auth == null ? "system" : auth.getName(), action, targetType, targetId);
    }
}
