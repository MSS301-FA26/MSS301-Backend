package com.sba301.cinemaai.dto.request.refund;

import jakarta.validation.constraints.Size;

public record CancelShowtimeRequest(
        @Size(max = 500, message = "Cancellation reason must be at most 500 characters")
        String reason
) {
}
