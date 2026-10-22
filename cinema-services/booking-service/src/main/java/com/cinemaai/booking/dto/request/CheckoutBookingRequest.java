package com.cinemaai.booking.dto.request;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

public record CheckoutBookingRequest(
        @Valid List<HoldSeatsRequest.TicketSelection> tickets,
        @Valid List<HoldSeatsRequest.FoodSelection> foods,
        @Min(0) Integer loyaltyPointsToRedeem
) {}
