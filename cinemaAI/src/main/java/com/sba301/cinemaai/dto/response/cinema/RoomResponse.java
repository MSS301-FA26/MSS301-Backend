package com.sba301.cinemaai.dto.response.cinema;

import com.sba301.cinemaai.enums.RoomStatus;
import com.sba301.cinemaai.enums.RoomType;
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
