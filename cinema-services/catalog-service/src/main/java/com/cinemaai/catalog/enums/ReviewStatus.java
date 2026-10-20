package com.cinemaai.catalog.enums;

public enum ReviewStatus {
    PUBLISHED,
    FLAGGED,
    HIDDEN,
    REJECTED,
    VISIBLE,
    DELETED;

    public boolean isPubliclyVisible() {
        return this == PUBLISHED || this == VISIBLE;
    }
}
