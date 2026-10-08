package com.cinemaai.catalog.dto.response;

import com.cinemaai.catalog.util.MessageTranslator;

public record FieldErrorResponse(
        String field,
        String message
) {
    public FieldErrorResponse {
        message = MessageTranslator.translate(message);
    }
}
