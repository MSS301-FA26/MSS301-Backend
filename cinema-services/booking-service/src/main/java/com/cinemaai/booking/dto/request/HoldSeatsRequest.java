package com.cinemaai.booking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record HoldSeatsRequest(
        @NotNull @Positive Long showtimeId,
        @NotEmpty List<@NotNull @Positive Long> seatIds,
        Boolean holiday,
        List<TicketSelection> tickets,
        List<FoodSelection> foods,
        Integer loyaltyPointsToRedeem
) {
    public record TicketSelection(Long seatId, String ticketType, Integer viewerAge, Integer quantity) {}
    public record FoodSelection(Long productId, Boolean isCombo, Integer quantity) {}
}
