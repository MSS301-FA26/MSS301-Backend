package com.sba301.cinemaai.enums;

public enum FoodItemStatus {
    DRAFT,
    ACTIVE,
    INACTIVE,
    ARCHIVED,
    // Retained for backward-compatibility with legacy client snapshots
    LOW_STOCK,
    OUT_OF_STOCK
}
