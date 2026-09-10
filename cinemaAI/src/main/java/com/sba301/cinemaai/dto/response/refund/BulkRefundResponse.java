package com.sba301.cinemaai.dto.response.refund;

public record BulkRefundResponse(
        Long showtimeId,
        int totalBookingsProcessed,
        int successCount,
        int failedCount,
        int pendingCount,
        String status,
        String estimatedCompletionTime
) {
}
