package com.cinemaai.identity.dto.response;

public record FieldErrorResponse(
        String field,
        String message
) {
}
