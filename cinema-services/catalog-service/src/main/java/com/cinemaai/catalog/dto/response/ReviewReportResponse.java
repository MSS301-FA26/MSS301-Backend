package com.cinemaai.catalog.dto.response;

import com.cinemaai.catalog.entity.ReviewReport;

import java.time.LocalDateTime;

public record ReviewReportResponse(
        Long id,
        Long reviewId,
        Long reporterUserId,
        String reporterEmail,
        String reason,
        String reasonLabel,
        String description,
        String status,
        LocalDateTime createdAt
) {
    public static ReviewReportResponse from(ReviewReport report) {
        if (report == null) return null;
        return new ReviewReportResponse(
                report.getId(),
                report.getReview() != null ? report.getReview().getId() : null,
                report.getReporterUserId(),
                report.getReporterEmail(),
                report.getReason().name(),
                report.getReason().getDescriptionVi(),
                report.getDescription(),
                report.getStatus().name(),
                report.getCreatedAt()
        );
    }
}
