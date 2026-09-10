package com.sba301.cinemaai.dto.response.ticket;

import java.math.BigDecimal;
import java.util.List;

public record TicketPriceValidationResponse(
        Long showtimeId,
        Long movieId,
        String movieTitle,
        String ageRating,
        boolean eligible,
        BigDecimal ticketSubtotal,
        BigDecimal comboPrice,
        BigDecimal finalAmount,
        List<TicketLinePriceResponse> tickets,
        List<String> warnings
) {
}
