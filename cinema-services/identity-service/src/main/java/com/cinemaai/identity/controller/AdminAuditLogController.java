package com.cinemaai.identity.controller;

import com.cinemaai.identity.dto.response.ApiResponse;
import com.cinemaai.identity.dto.response.PageResponse;
import com.cinemaai.identity.dto.response.audit.AuditLogResponse;
import com.cinemaai.identity.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Admin - Audit Logs", description = "Lịch sử thao tác hệ thống cho Admin")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Xem danh sách audit logs có phân trang")
    public ApiResponse<PageResponse<AuditLogResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String targetType
    ) {
        return ApiResponse.success(auditLogService.getLogs(page, size, targetType));
    }
}
