package com.sba301.cinemaai.dto.request.cinema;

import com.sba301.cinemaai.enums.SeatStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Operational change only; managers cannot alter the seat layout or seat type. */
public record SeatOperationalStatusRequest(
        @NotNull SeatStatus status,
        @NotBlank String reason
) {}
