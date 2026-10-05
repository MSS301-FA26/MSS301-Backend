package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.banner.CreateHeroBannerRequest;
import com.cinemaai.catalog.dto.request.banner.UpdateHeroBannerRequest;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.banner.HeroBannerResponse;
import com.cinemaai.catalog.enums.BannerScopeType;
import com.cinemaai.catalog.enums.BannerStatus;
import com.cinemaai.catalog.enums.BannerType;
import com.cinemaai.catalog.security.AuthenticatedUser;
import java.util.List;
import java.util.Map;

public interface HeroBannerService {

    List<HeroBannerResponse> getPublicBanners(Long cinemaId);

    void recordImpression(Long bannerId);

    void recordClick(Long bannerId);

    PageResponse<HeroBannerResponse> getAdminBanners(
            String search,
            BannerStatus status,
            BannerType type,
            BannerScopeType scopeType,
            Long cinemaId,
            int page,
            int size,
            AuthenticatedUser currentUser
    );

    HeroBannerResponse getAdminBannerById(Long id, AuthenticatedUser currentUser);

    HeroBannerResponse createBanner(CreateHeroBannerRequest request, AuthenticatedUser currentUser);

    HeroBannerResponse updateBanner(Long id, UpdateHeroBannerRequest request, AuthenticatedUser currentUser);

    HeroBannerResponse publishBanner(Long id, AuthenticatedUser currentUser);

    HeroBannerResponse pauseBanner(Long id, AuthenticatedUser currentUser);

    HeroBannerResponse resumeBanner(Long id, AuthenticatedUser currentUser);

    HeroBannerResponse archiveBanner(Long id, AuthenticatedUser currentUser);

    HeroBannerResponse duplicateBanner(Long id, AuthenticatedUser currentUser);

    void reorderBanners(List<Long> bannerIds, AuthenticatedUser currentUser);

    void deleteDraftBanner(Long id, AuthenticatedUser currentUser);

    Map<String, Object> getBannerSummaryStats();

    com.cinemaai.catalog.dto.response.banner.HeroSlotSettingsDto getSlotSettings();

    com.cinemaai.catalog.dto.response.banner.HeroSlotSettingsDto updateSlotSettings(
            com.cinemaai.catalog.dto.response.banner.HeroSlotSettingsDto request,
            AuthenticatedUser currentUser
    );

    List<com.cinemaai.catalog.dto.response.banner.HeroSlotDto> getHeroSlots(Long cinemaId);

    HeroBannerResponse pinAutoSlot(int position, Long cinemaId, AuthenticatedUser currentUser);

    void unassignSlot(int position, AuthenticatedUser currentUser);

    void assignBannerToSlot(int position, Long bannerId, AuthenticatedUser currentUser);
}
