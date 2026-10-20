package com.cinemaai.catalog.dto.response.cinema;

public record InternalCinemaResponse(
        Long id,
        String name,
        String status,
        boolean active
) {}
