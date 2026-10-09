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
    public record FoodSelection(
            Long productId,
            Boolean isCombo,
            Integer quantity,
            Long foodItemId,
            Long foodComboId
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
            if (quantity == null || quantity <= 0) {
                quantity = 1;
            }
        }
    }
}
