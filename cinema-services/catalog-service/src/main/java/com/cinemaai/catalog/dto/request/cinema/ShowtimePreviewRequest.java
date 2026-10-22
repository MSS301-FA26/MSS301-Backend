package com.cinemaai.catalog.dto.request.cinema;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request to preview ticket prices for a set of draft showtime slots.
 * This does NOT persist any data; it only calculates and returns the price matrix.
 */
public record ShowtimePreviewRequest(

        @NotNull(message = "Movie id is required")
        Long movieId,

        @NotNull(message = "At least one slot is required")
        List<PreviewSlot> slots
) {

    public record PreviewSlot(
            @NotNull(message = "Room id is required")
            Long roomId,

            @NotNull(message = "Start time is required")
            LocalDateTime startTime,

            /** Optional temp ID from frontend draft state (echoed back in response). */
            String tempId,

            Boolean weekendSurcharge,
            Boolean holidaySurcharge,
            java.math.BigDecimal lateNightSurchargeAmount
    ) {
        public PreviewSlot(Long roomId, LocalDateTime startTime, String tempId) {
            this(roomId, startTime, tempId, null, null, null);
        }
    }
}
