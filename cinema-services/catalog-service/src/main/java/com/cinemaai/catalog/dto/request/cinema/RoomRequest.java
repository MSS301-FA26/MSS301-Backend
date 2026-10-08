package com.cinemaai.catalog.dto.request.cinema;

import com.cinemaai.catalog.enums.RoomStatus;
import com.cinemaai.catalog.enums.RoomType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RoomRequest(
        Long cinemaId,

        @NotBlank(message = "Room name is required")
        String name,

        @NotNull(message = "Room type is required")
        RoomType roomType,

        @Min(value = 1, message = "Row count must be positive")
        @Max(value = 52, message = "Row count must be at most 52")
        int rowCount,

        @Min(value = 1, message = "Column count must be positive")
        @Max(value = 50, message = "Column count must be at most 50")
        int columnCount,

        RoomStatus status,

        BigDecimal standardPrice,
        BigDecimal vipPrice,
        BigDecimal couplePrice,
        Integer aislePosition
) {
}
