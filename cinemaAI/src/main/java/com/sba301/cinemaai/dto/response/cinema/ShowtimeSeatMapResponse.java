package com.sba301.cinemaai.dto.response.cinema;

import java.util.List;

public record ShowtimeSeatMapResponse(
        ShowtimeResponse showtime,
        int rowCount,
        int columnCount,
        List<ShowtimeSeatResponse> seats
) {
}
