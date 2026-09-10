package com.sba301.cinemaai.dto.response;

import com.sba301.cinemaai.util.MessageTranslator;

public record FieldErrorResponse(
        String field,
        String message
) {
    public FieldErrorResponse {
        message = MessageTranslator.translate(message);
    }
}
