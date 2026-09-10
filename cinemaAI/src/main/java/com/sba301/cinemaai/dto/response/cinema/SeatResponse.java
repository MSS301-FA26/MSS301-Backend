package com.sba301.cinemaai.dto.response.cinema;

import com.sba301.cinemaai.enums.SeatStatus;
import com.sba301.cinemaai.enums.SeatType;

public record SeatResponse(
        Long id,
        Long roomId,
        Long seatRowId,
        String rowLabel,
        int displayOrder,
        int seatNumber,
        int displayColumn,
        int startColumn,
        SeatType seatType,
        SeatStatus status
) {
}
