package com.cinemaai.booking.dto.request;

import java.util.List;

public record CheckoutBookingRequest(
        List<HoldSeatsRequest.TicketSelection> tickets,
        List<HoldSeatsRequest.FoodSelection> foods,
        Integer loyaltyPointsToRedeem
) {}
