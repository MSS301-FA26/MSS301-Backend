package com.cinemaai.catalog.dto.response.cinema;

public record RoomLayoutConfigResponse(
        Long roomId,
        String roomName,
        Integer rowCount,
        Integer columnCount,
        Integer aislePosition
) {
}
