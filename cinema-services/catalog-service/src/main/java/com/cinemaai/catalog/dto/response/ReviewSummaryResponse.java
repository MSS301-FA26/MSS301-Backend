package com.cinemaai.catalog.dto.response;

import java.util.Map;

public record ReviewSummaryResponse(
        Long movieId,
        double averageRating,
        long totalReviews,
        long verifiedCount,
        double verifiedRatio,
        Map<String, RatingDistributionItem> distribution
) {
    public record RatingDistributionItem(
            long count,
            double percentage
    ) {}
}
