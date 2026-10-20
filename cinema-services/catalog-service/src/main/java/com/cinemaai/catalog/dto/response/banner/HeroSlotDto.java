package com.cinemaai.catalog.dto.response.banner;

import java.util.Map;

public record HeroSlotDto(
        int position,
        String assignmentType, // "MANUAL", "AUTO", "EMPTY"
        boolean enabled,
        HeroBannerResponse banner,
        Map<String, Object> autoMovie
) {
}
