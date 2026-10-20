package com.cinemaai.catalog.dto.request.review;

import com.cinemaai.catalog.enums.ReviewReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportReviewRequest(
        @NotNull(message = "Lý do báo cáo không được để trống")
        ReviewReportReason reason,

        @Size(max = 1000, message = "Mô tả chi tiết tối đa 1000 ký tự")
        String description
) {}
