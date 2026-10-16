package com.cinemaai.payment.dto.request;

import java.math.BigDecimal;
import java.util.List;

public record CreateRefundBatchRequest(
        Long cinemaId,
        Long showtimeId,
        String reason,
        List<BatchItem> items
) {
    public record BatchItem(
            Long bookingId,
            Long userId,
            BigDecimal amount,
            String bookingCode
    ) {}
}
