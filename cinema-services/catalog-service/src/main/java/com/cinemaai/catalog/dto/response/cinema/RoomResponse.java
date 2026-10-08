package com.cinemaai.catalog.dto.response.cinema;

import com.cinemaai.catalog.enums.RoomStatus;
import com.cinemaai.catalog.enums.RoomType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RoomResponse(
        Long id,
        Long cinemaId,
        String cinemaName,
        String name,
        RoomType roomType,
        int rowCount,
        int columnCount,
        RoomStatus status,
        BigDecimal standardPrice,
        BigDecimal vipPrice,
        BigDecimal couplePrice,
        Integer aislePosition,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
