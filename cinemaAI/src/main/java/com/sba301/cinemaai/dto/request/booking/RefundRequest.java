package com.sba301.cinemaai.dto.request.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundRequest(
        @NotBlank(message = "Refund reason is required")
        @Size(max = 500, message = "Refund reason must be at most 500 characters")
        String reason
) {
}
