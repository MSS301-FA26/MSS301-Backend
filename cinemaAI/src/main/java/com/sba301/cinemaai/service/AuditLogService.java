package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.audit.AuditLogResponse;
import com.sba301.cinemaai.enums.AuditActionType;

public interface AuditLogService {

    /**
     * Ghi một dòng audit cho hành động của admin/staff hiện tại.
     * Không bao giờ ném exception — audit lỗi không được phá nghiệp vụ chính.
     */
    void record(AuditActionType action, String targetType, Long targetId, String detail);

    void record(AuditActionType action, String targetType, Long targetId, String detail, Long cinemaId);

    PageResponse<AuditLogResponse> getLogs(int page, int size, String targetType);

    PageResponse<AuditLogResponse> getLogsByCinema(Long cinemaId, int page, int size);
}
