package com.cinemaai.catalog.dto.request.quote;

import com.cinemaai.catalog.enums.TicketType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CheckoutQuoteRequest(
        @NotNull @Positive Long showtimeId,
        @NotEmpty @Size(max = 100) List<@NotNull @Positive Long> seatIds,
        @Size(max = 100) List<@Valid Ticket> tickets,
        @Size(max = 100) List<@Valid Food> foods,
        String voucherCode,
        Integer cinePointsToUse,
        Long bookingSessionId
) {
    public CheckoutQuoteRequest(Long showtimeId, List<Long> seatIds, List<Ticket> tickets, List<Food> foods) {
        this(showtimeId, seatIds, tickets, foods, null, null, null);
    }

    public CheckoutQuoteRequest {
        if (foods == null) foods = List.of();
        if (tickets == null) tickets = List.of();
    }

    // seatId is optional for batch clients; without it seats are assigned in input order.
    public record Ticket(
            @Positive Long seatId,
            TicketType ticketType,
            Integer viewerAge,
            Integer quantity
    ) {
        public Ticket(Long seatId, TicketType ticketType, Integer viewerAge, Integer quantity) {
            this.seatId = seatId;
            this.ticketType = ticketType != null ? ticketType : TicketType.ADULT;
            this.viewerAge = viewerAge != null ? viewerAge : 22;
            this.quantity = quantity != null && quantity > 0 ? quantity : 1;
        }
    }

    public record Food(
            Long productId,
            Boolean isCombo,
            Integer quantity,
            Long foodItemId,
            Long foodComboId
    ) {
        public Food(Long productId, Boolean isCombo, Integer quantity) {
            this(productId, isCombo, quantity, null, null);
        }

        public Food {
            if (quantity == null || quantity <= 0) quantity = 1;
            if (productId == null) {
                if (foodComboId != null) {
                    productId = foodComboId;
                    isCombo = true;
                } else if (foodItemId != null) {
                    productId = foodItemId;
                    isCombo = false;
                }
            }
            if (isCombo == null) {
                isCombo = false;
            }
        }
    }
}
