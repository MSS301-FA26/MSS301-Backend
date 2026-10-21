package com.cinemaai.booking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record HoldSeatsRequest(
        @NotNull @Positive Long showtimeId,
        @NotEmpty List<@NotNull @Positive Long> seatIds,
        Boolean holiday,
        List<@Valid TicketSelection> tickets,
        List<@Valid FoodSelection> foods,
        @Min(0) Integer loyaltyPointsToRedeem
) {
    public record TicketSelection(
            @Positive Long seatId,
            @NotBlank String ticketType,
            @NotNull @Min(0) Integer viewerAge,
            @NotNull @Positive Integer quantity
    ) {}
    public record FoodSelection(
            @Positive Long productId,
            Boolean isCombo,
            @NotNull @Positive Integer quantity,
            @Positive Long foodItemId,
            @Positive Long foodComboId
    ) {
        public FoodSelection(Long productId, Boolean isCombo, Integer quantity) {
            this(productId, isCombo, quantity, null, null);
        }

        public FoodSelection {
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
