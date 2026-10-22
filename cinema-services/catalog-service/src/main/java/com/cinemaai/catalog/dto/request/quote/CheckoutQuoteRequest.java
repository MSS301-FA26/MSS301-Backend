package com.cinemaai.catalog.dto.request.quote;

import com.cinemaai.catalog.enums.TicketType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CheckoutQuoteRequest(
        @NotNull @Positive Long showtimeId,
        @NotEmpty @Size(max = 100) List<@NotNull @Positive Long> seatIds,
        @Size(max = 100) List<@Valid Ticket> tickets,
        @Size(max = 100) List<@Valid ProductSelection> foods,
        String voucherCode,
        Integer cinePointsToUse,
        Long bookingSessionId
) {
    public CheckoutQuoteRequest(Long showtimeId, List<Long> seatIds, List<Ticket> tickets, List<ProductSelection> foods) {
        this(showtimeId, seatIds, tickets, foods, null, null, null);
    }

    @JsonCreator
    public CheckoutQuoteRequest(
            @JsonProperty("showtimeId") Long showtimeId,
            @JsonProperty("seatIds") List<Long> seatIds,
            @JsonProperty("tickets") List<Ticket> tickets,
            @JsonProperty("foods") List<ProductSelection> foods,
            @JsonProperty("products") List<ProductSelection> products,
            @JsonProperty("voucherCode") String voucherCode,
            @JsonProperty("cinePointsToUse") Integer cinePointsToUse,
            @JsonProperty("bookingSessionId") Long bookingSessionId
    ) {
        this(showtimeId, seatIds, tickets, normalizeProducts(foods, products), voucherCode,
                cinePointsToUse, bookingSessionId);
    }

    public CheckoutQuoteRequest {
        if (foods == null) foods = List.of();
        if (tickets == null) tickets = List.of();
    }

    // seatId is optional for batch clients; without it seats are assigned in input order.
    public record Ticket(
            @Positive Long seatId,
            @NotNull TicketType ticketType,
            @NotNull @PositiveOrZero Integer viewerAge,
            @NotNull @Positive Integer quantity
    ) {
        public Ticket(Long seatId, TicketType ticketType, Integer viewerAge, Integer quantity) {
            this.seatId = seatId;
            this.ticketType = ticketType;
            this.viewerAge = viewerAge;
            this.quantity = quantity;
        }
    }

    private static List<ProductSelection> normalizeProducts(
            List<ProductSelection> foods,
            List<ProductSelection> products
    ) {
        if (foods != null && products != null) {
            throw new IllegalArgumentException("Specify either foods or products, not both");
        }
        return foods != null ? foods : products;
    }

    public record ProductSelection(
            Long productId,
            Boolean isCombo,
            Integer quantity,
            Long foodItemId,
            Long foodComboId
    ) {
        public ProductSelection(Long productId, Boolean isCombo, Integer quantity) {
            this(productId, isCombo, quantity, null, null);
        }

        public ProductSelection {
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
