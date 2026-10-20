package com.sba301.cinemaai.dto.response.banner;

import com.sba301.cinemaai.enums.BannerCtaType;
import com.sba301.cinemaai.enums.BannerFocalPoint;
import com.sba301.cinemaai.enums.BannerScopeType;
import com.sba301.cinemaai.enums.BannerStatus;
import com.sba301.cinemaai.enums.BannerType;
import java.time.LocalDateTime;
import java.util.List;

public record HeroBannerResponse(
        Long id,
        String name,
        BannerType type,
        String title,
        String subtitle,
        String description,
        String badge,
        String formatLabel,

        // Canonical image fields
        String desktopImageUrl,
        String mobileImageUrl,
        String altText,
        BannerFocalPoint focalPoint,

        // Aliases for seamless frontend carousel compatibility
        String imageDesktop,
        String imageMobile,
        String format,

        // Linked movie info
        Long linkedMovieId,
        Long movieId,
        String movieTitle,
        String moviePosterUrl,
        String movieTrailerUrl,
        Integer movieDuration,
        String movieAgeRating,
        String movieStatus,

        // Linked promotion info
        Long linkedPromotionId,
        String promotionCode,
        String promotionName,

        // CTA buttons
        BannerCtaType primaryCtaType,
        String primaryCtaLabel,
        String primaryCtaUrl,
        BannerCtaType secondaryCtaType,
        String secondaryCtaLabel,
        String secondaryCtaUrl,

        // Aliases for frontend
        String ctaLabel,
        String ctaUrl,

        // Scope
        BannerScopeType scopeType,
        String targetCity,
        List<BannerCinemaSummary> cinemas,

        // Ordering & Duration
        int priority,
        int sortOrder,
        int slideDurationSeconds,

        // Lifecycle & Schedule
        LocalDateTime startAt,
        LocalDateTime endAt,
        BannerStatus status,

        // Analytics
        long impressionsCount,
        long clicksCount,
        double ctr,

        // Audit & Metadata
        Long createdById,
        String createdByName,
        Long updatedById,
        String updatedByName,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record BannerCinemaSummary(Long id, String name, String city) {}
}
