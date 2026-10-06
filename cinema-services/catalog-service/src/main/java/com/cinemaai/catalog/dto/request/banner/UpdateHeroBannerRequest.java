package com.cinemaai.catalog.dto.request.banner;

import com.cinemaai.catalog.enums.BannerCtaType;
import com.cinemaai.catalog.enums.BannerFocalPoint;
import com.cinemaai.catalog.enums.BannerScopeType;
import com.cinemaai.catalog.enums.BannerType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record UpdateHeroBannerRequest(
        @NotBlank(message = "Tên banner nội bộ không được để trống")
        String name,

        @NotNull(message = "Loại banner không được để trống")
        BannerType type,

        String title,

        Boolean isImageOnly,

        String subtitle,
        String description,
        String badge,
        String formatLabel,

        @NotBlank(message = "Ảnh desktop không được để trống")
        String desktopImageUrl,

        String mobileImageUrl,
        String altText,
        BannerFocalPoint focalPoint,

        Long linkedMovieId,
        Long linkedPromotionId,
        Long linkedFnbId,
        String fnbType,

        @NotNull(message = "Loại nút CTA chính không được để trống")
        BannerCtaType primaryCtaType,

        @NotBlank(message = "Nhãn nút CTA chính không được để trống")
        String primaryCtaLabel,

        String primaryCtaUrl,

        BannerCtaType secondaryCtaType,
        String secondaryCtaLabel,
        String secondaryCtaUrl,

        @NotNull(message = "Phạm vi hiển thị không được để trống")
        BannerScopeType scopeType,

        String targetCity,
        List<Long> cinemaIds,

        Integer priority,
        Integer sortOrder,

        @Min(value = 4, message = "Thời gian hiển thị tối thiểu 4 giây")
        @Max(value = 10, message = "Thời gian hiển thị tối đa 10 giây")
        Integer slideDurationSeconds,

        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
