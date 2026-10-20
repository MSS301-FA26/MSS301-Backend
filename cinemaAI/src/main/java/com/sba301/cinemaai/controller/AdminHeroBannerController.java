package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.banner.CreateHeroBannerRequest;
import com.sba301.cinemaai.dto.request.banner.ReorderHeroBannersRequest;
import com.sba301.cinemaai.dto.request.banner.UpdateHeroBannerRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.banner.HeroBannerResponse;
import com.sba301.cinemaai.dto.response.banner.HotMovieSuggestionResponse;
import com.sba301.cinemaai.enums.BannerScopeType;
import com.sba301.cinemaai.enums.BannerStatus;
import com.sba301.cinemaai.enums.BannerType;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.HeroBannerService;
import com.sba301.cinemaai.service.HotMovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/hero-banners")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Hero Banners", description = "Management endpoints for hero banners/carousel")
public class AdminHeroBannerController {

    private final HeroBannerService heroBannerService;
    private final HotMovieService hotMovieService;

    @GetMapping
    @Operation(summary = "Get paginated hero banners with filters")
    public ApiResponse<PageResponse<HeroBannerResponse>> getAdminBanners(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BannerStatus status,
            @RequestParam(required = false) BannerType type,
            @RequestParam(required = false) BannerScopeType scopeType,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(
                heroBannerService.getAdminBanners(search, status, type, scopeType, cinemaId, page, size, currentUser),
                "Lấy danh sách Hero Banner thành công"
        );
    }

    @GetMapping("/summary")
    @Operation(summary = "Get hero banner summary counts and CTR statistics")
    public ApiResponse<Map<String, Object>> getSummaryStats() {
        return ApiResponse.success(
                heroBannerService.getBannerSummaryStats(),
                "Lấy thống kê Hero Banner thành công"
        );
    }

    @GetMapping("/hot-movie-suggestions")
    @Operation(summary = "Get hot movie suggestions based on real bookings, occupancy, and ratings")
    public ApiResponse<List<HotMovieSuggestionResponse>> getHotMovieSuggestions(
            @RequestParam(defaultValue = "6") int limit
    ) {
        return ApiResponse.success(
                hotMovieService.getHotMovieSuggestions(limit),
                "Lấy danh sách đề xuất phim hot thành công"
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hero banner detail by ID")
    public ApiResponse<HeroBannerResponse> getBannerById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(
                heroBannerService.getAdminBannerById(id, currentUser),
                "Lấy chi tiết Hero Banner thành công"
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new hero banner")
    public ApiResponse<HeroBannerResponse> createBanner(
            @Valid @RequestBody CreateHeroBannerRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(
                heroBannerService.createBanner(request, currentUser),
                "Tạo Hero Banner thành công"
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update hero banner")
    public ApiResponse<HeroBannerResponse> updateBanner(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHeroBannerRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(
                heroBannerService.updateBanner(id, request, currentUser),
                "Cập nhật Hero Banner thành công"
        );
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish hero banner immediately")
    public ApiResponse<HeroBannerResponse> publishBanner(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(
                heroBannerService.publishBanner(id, currentUser),
                "Xuất bản Hero Banner thành công"
        );
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause hero banner")
    public ApiResponse<HeroBannerResponse> pauseBanner(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(
                heroBannerService.pauseBanner(id, currentUser),
                "Tạm dừng Hero Banner thành công"
        );
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume paused hero banner")
    public ApiResponse<HeroBannerResponse> resumeBanner(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(
                heroBannerService.resumeBanner(id, currentUser),
                "Tiếp tục chạy Hero Banner thành công"
        );
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive hero banner")
    public ApiResponse<HeroBannerResponse> archiveBanner(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(
                heroBannerService.archiveBanner(id, currentUser),
                "Lưu trữ Hero Banner thành công"
        );
    }

    @PostMapping("/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Duplicate hero banner")
    public ApiResponse<HeroBannerResponse> duplicateBanner(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(
                heroBannerService.duplicateBanner(id, currentUser),
                "Nhân bản Hero Banner thành công"
        );
    }

    @PutMapping("/reorder")
    @Operation(summary = "Reorder hero banners")
    public ApiResponse<Void> reorderBanners(
            @Valid @RequestBody ReorderHeroBannersRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        heroBannerService.reorderBanners(request.bannerIds(), currentUser);
        return ApiResponse.success(null, "Cập nhật thứ tự banner thành công");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete draft hero banner")
    public ApiResponse<Void> deleteDraftBanner(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        heroBannerService.deleteDraftBanner(id, currentUser);
        return ApiResponse.success(null, "Xóa bản nháp banner thành công");
    }
}
