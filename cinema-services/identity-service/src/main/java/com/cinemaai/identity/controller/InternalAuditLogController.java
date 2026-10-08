package com.cinemaai.identity.controller;

import com.cinemaai.identity.dto.response.ApiResponse;
import com.cinemaai.identity.enums.AuditActionType;
import com.cinemaai.identity.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Internal - Audit Logs", description = "Internal service-to-service audit logging")
public class InternalAuditLogController {

    private final AuditLogService auditLogService;

    public record InternalAuditRequest(
            String action,
            String targetType,
            Long targetId,
            String detail,
            Long actorId
    ) {}

    @PostMapping
    @Operation(summary = "Ghi audit log từ microservice khác")
    public ApiResponse<Void> record(@RequestBody InternalAuditRequest request) {
        AuditActionType actionType;
        try {
            actionType = AuditActionType.valueOf(request.action().toUpperCase());
        } catch (Exception e) {
            actionType = AuditActionType.UPDATE;
        }
        auditLogService.recordInternal(actionType, request.targetType(), request.targetId(), request.detail(), request.actorId());
        return ApiResponse.success(null);
    }
}
