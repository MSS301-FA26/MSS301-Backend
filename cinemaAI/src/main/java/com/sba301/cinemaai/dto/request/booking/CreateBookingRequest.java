package com.sba301.cinemaai.dto.request.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.sba301.cinemaai.dto.request.ticket.TicketSelectionRequest;
import java.util.List;

public record CreateBookingRequest(
        @NotNull(message = "Hold booking id is required")
        Long holdBookingId,

        @Valid
        List<BookingFoodRequest> foods,

        Long comboId,

        boolean holiday,

        @Valid
        List<TicketSelectionRequest> tickets,

        Integer loyaltyPointsToRedeem
) {
    public CreateBookingRequest(Long holdBookingId, List<BookingFoodRequest> foods) {
        this(holdBookingId, foods, null, false, null, null);
    }
}
