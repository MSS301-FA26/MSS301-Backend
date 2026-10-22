package com.cinemaai.booking.client.dto;

import java.time.LocalDateTime;

public record ReportingShowtimeDto(
        Long showtimeId, Long movieId, String movieTitle, Long cinemaId, String cinemaName,
        Long roomId, String roomName, LocalDateTime startTime, LocalDateTime endTime, long capacity
) {}
