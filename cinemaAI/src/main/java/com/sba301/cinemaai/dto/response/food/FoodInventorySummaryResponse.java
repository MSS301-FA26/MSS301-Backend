package com.sba301.cinemaai.dto.response.food;

/** Per-cinema operational F&B stock summary. It deliberately contains no global catalogue edits. */
public record FoodInventorySummaryResponse(
        Long cinemaId,
        int trackedItems,
        int availableUnits,
        int lowStockItems,
        int outOfStockItems
) {}
