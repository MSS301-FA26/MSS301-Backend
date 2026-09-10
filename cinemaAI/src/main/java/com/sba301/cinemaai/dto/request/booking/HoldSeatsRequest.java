package com.sba301.cinemaai.dto.request.booking;

import com.sba301.cinemaai.dto.request.ticket.TicketSelectionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record HoldSeatsRequest(
        @NotNull(message = "Showtime id is required")
        Long showtimeId,

        @NotEmpty(message = "At least one seat is required")
        List<Long> seatIds,

        Long comboId,

        boolean holiday,

        @Valid
        List<TicketSelectionRequest> tickets,

        @Valid
        List<BookingFoodRequest> foods,

        Integer loyaltyPointsToRedeem
) {
    public HoldSeatsRequest(Long showtimeId, List<Long> seatIds) {
        this(showtimeId, seatIds, null, false, null, null, null);
    }
}
