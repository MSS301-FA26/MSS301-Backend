package com.sba301.cinemaai.dto.response;

import com.sba301.cinemaai.util.MessageTranslator;
import java.time.LocalDateTime;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        LocalDateTime timestamp
) {

    public ApiResponse {
        message = MessageTranslator.translate(message);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "Success", LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(false, null, message, LocalDateTime.now());
    }
}
