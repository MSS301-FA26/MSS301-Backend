package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.banner.CreateHeroBannerRequest;
import com.sba301.cinemaai.dto.request.banner.UpdateHeroBannerRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.banner.HeroBannerResponse;
import com.sba301.cinemaai.enums.BannerScopeType;
import com.sba301.cinemaai.enums.BannerStatus;
import com.sba301.cinemaai.enums.BannerType;
import com.sba301.cinemaai.security.AuthenticatedUser;
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
}
