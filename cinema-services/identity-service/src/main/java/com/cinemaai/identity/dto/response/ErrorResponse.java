package com.cinemaai.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
