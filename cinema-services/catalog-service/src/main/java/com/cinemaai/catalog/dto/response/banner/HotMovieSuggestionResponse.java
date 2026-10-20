package com.cinemaai.catalog.dto.response.banner;

import java.math.BigDecimal;

public record HotMovieSuggestionResponse(
        Long movieId,
        String title,
        String posterUrl,
        String bannerUrl,
        String trailerUrl,
        int durationMinutes,
        String ageRating,
        String status,
        long ticketsSold,
        long bookingCount,
        BigDecimal revenue,
        double occupancyRate,
        double averageRating,
        long reviewCount,
        long wishlistCount,
        double hotScore,
        boolean isCurrentlyOnHero
) {
}
