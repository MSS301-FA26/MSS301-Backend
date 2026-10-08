package com.cinemaai.catalog.dto.response;

public record AdminReviewStatsResponse(
        long totalReviews,
        double averageRating,
        long flaggedCount,
        long hiddenCount,
        double verifiedRatio
) {}
