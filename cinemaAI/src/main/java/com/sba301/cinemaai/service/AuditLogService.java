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

    /**
     * Trả về audit log phân trang, mới nhất trước.
     * {@code targetType} là tiền tố để lọc (vd. "MOVIE" khớp cả "MOVIE_ACTOR"),
     * {@code null} hoặc blank = trả tất cả; tối đa 100 bản ghi mỗi trang.
     */
    PageResponse<AuditLogResponse> getLogs(int page, int size, String targetType);
}
