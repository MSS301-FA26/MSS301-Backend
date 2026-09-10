package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.audit.AuditLogResponse;
import com.sba301.cinemaai.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Audit Logs", description = "Who did what in the admin/staff area")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "List audit logs",
            description = "Paged audit trail, newest first. Optional targetType prefix filter (e.g. MOVIE, SHOWTIME, FOOD, BOOKING, GENRE, ACTOR, ROOM, USER, TICKET_PRICING, LOYALTY, REVIEW).")
    public ApiResponse<PageResponse<AuditLogResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String targetType
    ) {
        return ApiResponse.success(auditLogService.getLogs(page, size, targetType));
    }
}
