package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.audit.AuditLogResponse;
import com.sba301.cinemaai.entity.AuditLog;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.repository.AuditLogRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void record(AuditActionType action, String targetType, Long targetId, String detail) {
        try {
            User actor = resolveActor();
            auditLogRepository.save(new AuditLog(actor, action, targetType, targetId, detail, null));
        } catch (Exception e) {
            log.warn("Audit log write failed for {} {} #{}: {}", action, targetType, targetId, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getLogs(int page, int size, String targetType) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = (targetType == null || targetType.isBlank())
                ? auditLogRepository.findAll(pageable)
                : auditLogRepository.findByTargetTypeStartingWith(targetType.trim().toUpperCase(), pageable);
        return PageResponse.from(result.map(this::toResponse));
    }

    private User resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            return null;
        }
        return userRepository.findByEmail(principal.email()).orElse(null);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        User actor = auditLog.getActor();
        String actorName = null;
        if (actor != null && actor.getProfile() != null) {
            actorName = actor.getProfile().getFullName();
        }
        return new AuditLogResponse(
                auditLog.getId(),
                actor == null ? null : actor.getEmail(),
                actorName,
                auditLog.getAction(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getDetail(),
                auditLog.getCreatedAt()
        );
    }
}
