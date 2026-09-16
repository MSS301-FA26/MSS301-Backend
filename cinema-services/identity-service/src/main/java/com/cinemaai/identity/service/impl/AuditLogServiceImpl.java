package com.cinemaai.identity.service.impl;

import com.cinemaai.identity.dto.response.PageResponse;
import com.cinemaai.identity.dto.response.audit.AuditLogResponse;
import com.cinemaai.identity.entity.AuditLog;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.AuditActionType;
import com.cinemaai.identity.repository.AuditLogRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.security.AuthenticatedUser;
import com.cinemaai.identity.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditActionType action, String targetType, Long targetId, String detail) {
        try {
            User actor = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser authUser) {
                actor = userRepository.findById(authUser.id()).orElse(null);
            }

            AuditLog logEntry = new AuditLog(actor, action, targetType, targetId, detail, null);
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to record audit log for action={} targetType={} targetId={}: {}",
                    action, targetType, targetId, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getLogs(int page, int size, String targetType) {
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.min(Math.max(1, size), 100);
        PageRequest pageRequest = PageRequest.of(boundedPage, boundedSize);

        Page<AuditLog> logPage;
        if (targetType != null && !targetType.isBlank()) {
            logPage = auditLogRepository.findByTargetTypeStartingWithIgnoreCaseOrderByCreatedAtDesc(targetType.trim(), pageRequest);
        } else {
            logPage = auditLogRepository.findAllByOrderByCreatedAtDesc(pageRequest);
        }

        return PageResponse.from(logPage.map(AuditLogResponse::from));
    }
}
