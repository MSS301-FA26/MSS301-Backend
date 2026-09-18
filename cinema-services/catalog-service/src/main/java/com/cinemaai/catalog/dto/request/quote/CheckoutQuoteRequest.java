package com.cinemaai.catalog.dto.request.quote;

import com.cinemaai.catalog.enums.TicketType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CheckoutQuoteRequest(
        @NotNull @Positive Long showtimeId,
        @NotEmpty @Size(max = 100) List<@NotNull @Positive Long> seatIds,
        @NotEmpty @Size(max = 100) List<@NotNull @Valid Ticket> tickets,
        @Size(max = 100) List<@NotNull @Valid Food> foods
) {
    public CheckoutQuoteRequest {
        if (foods == null) foods = List.of();
    }
    // seatId is optional for batch clients; without it seats are assigned in input order.
    public record Ticket(@Positive Long seatId, @NotNull TicketType ticketType,
                         @NotNull @Min(0) @Max(120) Integer viewerAge,
                         @Min(1) @Max(100) int quantity) {}
    public record Food(@NotNull @Positive Long productId, @NotNull Boolean isCombo,
                       @Min(1) @Max(100) int quantity) {}
}
