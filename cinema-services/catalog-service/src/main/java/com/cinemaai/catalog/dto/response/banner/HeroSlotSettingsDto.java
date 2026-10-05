package com.cinemaai.catalog.dto.response.banner;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record HeroSlotSettingsDto(
        @JsonProperty("slotCount")
        @Min(value = 1, message = "Số lượng slot tối thiểu là 1")
        @Max(value = 10, message = "Số lượng slot tối đa là 10")
        int slotCount,

        @JsonProperty("autoFillEnabled")
        boolean autoFillEnabled,

        @JsonProperty("autoSourceNowShowing")
        boolean autoSourceNowShowing,

        @JsonProperty("autoSourceComingSoon")
        boolean autoSourceComingSoon,

        @JsonProperty("refreshMinutes")
        int refreshMinutes
) {
    public static HeroSlotSettingsDto defaults() {
        return new HeroSlotSettingsDto(5, true, true, false, 15);
    }
}
