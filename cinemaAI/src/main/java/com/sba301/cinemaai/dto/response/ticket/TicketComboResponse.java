package com.sba301.cinemaai.dto.response.ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketComboResponse(
        Long id,
        String name,
        String description,
        int adultCount,
        int childCount,
        int studentCount,
        BigDecimal price,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
