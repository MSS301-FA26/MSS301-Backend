package com.sba301.cinemaai.dto.request.ticket;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TicketComboRequest(
        @NotBlank(message = "Combo name is required")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @Min(value = 0, message = "Adult count must be zero or positive")
        int adultCount,

        @Min(value = 0, message = "Child count must be zero or positive")
        int childCount,

        @Min(value = 0, message = "Student count must be zero or positive")
        int studentCount,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "10000", message = "Price must be at least 10000")
        @DecimalMax(value = "1000000", message = "Price must be at most 1000000")
        BigDecimal price,

        Boolean active
) {
}
