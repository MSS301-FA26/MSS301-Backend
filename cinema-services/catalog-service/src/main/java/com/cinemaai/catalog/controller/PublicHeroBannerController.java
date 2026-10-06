package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.banner.HeroBannerResponse;
import com.cinemaai.catalog.service.HeroBannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hero-banners")
@RequiredArgsConstructor
@Tag(name = "Public - Hero Banners", description = "Public endpoints for Landing Page hero carousel")
public class PublicHeroBannerController {

    private final HeroBannerService heroBannerService;

    @GetMapping
    @Operation(summary = "Get active published hero banners for Landing Page")
    public ResponseEntity<ApiResponse<List<HeroBannerResponse>>> getPublicBanners(
            @RequestParam(required = false) Long cinemaId
    ) {
        List<HeroBannerResponse> banners = heroBannerService.getPublicBanners(cinemaId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, java.util.concurrent.TimeUnit.SECONDS).cachePublic())
                .body(ApiResponse.success(banners, "Lấy danh sách Hero Banner thành công"));
    }

    @PostMapping("/{id}/impression")
    @Operation(summary = "Track impression count for a hero banner")
    public ApiResponse<Void> trackImpression(@PathVariable Long id) {
        heroBannerService.recordImpression(id);
        return ApiResponse.success(null, "Tracked impression");
    }

    @PostMapping("/{id}/click")
    @Operation(summary = "Track CTA click count for a hero banner")
    public ApiResponse<Void> trackClick(@PathVariable Long id) {
        heroBannerService.recordClick(id);
        return ApiResponse.success(null, "Tracked click");
    }
}
