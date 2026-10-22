package com.cinemaai.catalog.dto.response.cinema;

import java.time.LocalDateTime;

public record ReportingShowtimeResponse(
        Long showtimeId, Long movieId, String movieTitle, Long cinemaId, String cinemaName,
        Long roomId, String roomName, LocalDateTime startTime, LocalDateTime endTime, long capacity
) {}
