package com.cinemaai.booking.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        boolean success,
        String message,
        String path,
        List<FieldErrorResponse> errors,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(String message, String path) {
        return new ErrorResponse(false, message, path, List.of(), LocalDateTime.now());
    }

    public static ErrorResponse of(String message, String path, List<FieldErrorResponse> errors) {
        return new ErrorResponse(false, message, path, errors, LocalDateTime.now());
    }
}
