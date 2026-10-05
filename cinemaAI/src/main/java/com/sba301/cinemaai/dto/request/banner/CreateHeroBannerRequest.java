package com.sba301.cinemaai.dto.request.banner;

import com.sba301.cinemaai.enums.BannerCtaType;
import com.sba301.cinemaai.enums.BannerFocalPoint;
import com.sba301.cinemaai.enums.BannerScopeType;
import com.sba301.cinemaai.enums.BannerType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record CreateHeroBannerRequest(
        @NotBlank(message = "Tên banner nội bộ không được để trống")
        String name,

        @NotNull(message = "Loại banner không được để trống")
        BannerType type,

        @NotBlank(message = "Tiêu đề banner không được để trống")
        String title,

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
        LocalDateTime endAt,
        Boolean publishNow
) {
}
