package com.cinemaai.catalog.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MovieStatus {
    UPCOMING,
    NOW_SHOWING,
    ENDED,
    INACTIVE;

    @JsonCreator
    public static MovieStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return UPCOMING;
        }
        String normalized = value.trim().toUpperCase();
        if ("COMING_SOON".equals(normalized) || "UPCOMING".equals(normalized)) {
            return UPCOMING;
        }
        for (MovieStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        return UPCOMING;
    }
}
