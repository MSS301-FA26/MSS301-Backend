package com.cinemaai.catalog.service.impl;

import com.cinemaai.catalog.dto.request.banner.CreateHeroBannerRequest;
import com.cinemaai.catalog.dto.request.banner.UpdateHeroBannerRequest;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.banner.HeroBannerResponse;
import com.cinemaai.catalog.dto.response.banner.HeroBannerResponse.BannerCinemaSummary;
import com.cinemaai.catalog.dto.response.banner.HeroSlotDto;
import com.cinemaai.catalog.dto.response.banner.HeroSlotSettingsDto;
import com.cinemaai.catalog.entity.Cinema;
import com.cinemaai.catalog.entity.HeroBanner;
import com.cinemaai.catalog.entity.HeroBannerCinema;
import com.cinemaai.catalog.entity.Movie;
import com.cinemaai.catalog.entity.SystemSetting;
import com.cinemaai.catalog.enums.AuditActionType;
import com.cinemaai.catalog.enums.BannerCtaType;
import com.cinemaai.catalog.enums.BannerFocalPoint;
import com.cinemaai.catalog.enums.BannerScopeType;
import com.cinemaai.catalog.enums.BannerStatus;
import com.cinemaai.catalog.enums.BannerType;
import com.cinemaai.catalog.enums.MovieStatus;
import com.cinemaai.catalog.exception.BadRequestException;
import com.cinemaai.catalog.exception.ForbiddenException;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.repository.CinemaRepository;
import com.cinemaai.catalog.repository.HeroBannerCinemaRepository;
import com.cinemaai.catalog.repository.HeroBannerRepository;
import com.cinemaai.catalog.repository.MovieRepository;
import com.cinemaai.catalog.repository.SystemSettingRepository;
import com.cinemaai.catalog.security.AuthenticatedUser;
import com.cinemaai.catalog.security.CinemaSecurityService;
import com.cinemaai.catalog.service.AuditLogService;
import com.cinemaai.catalog.service.HeroBannerService;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeroBannerServiceImpl implements HeroBannerService {

    private final HeroBannerRepository heroBannerRepository;
    private final HeroBannerCinemaRepository heroBannerCinemaRepository;
    private final MovieRepository movieRepository;
    private final CinemaRepository cinemaRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final CinemaSecurityService cinemaSecurityService;
    private final AuditLogService auditLogService;

    @Value("${app.hero.banner.max-active:7}")
    private int maxActiveBanners = 7;

    private static final int MIN_DURATION = 4;
    private static final int MAX_DURATION = 10;
    private static final int DEFAULT_DURATION = 6;

    // ==========================================
    // 1. PUBLIC API
    // ==========================================

    @Override
    @Transactional
    public List<HeroBannerResponse> getPublicBanners(Long cinemaId) {
        LocalDateTime now = LocalDateTime.now();
        HeroSlotSettingsDto settings = getSlotSettings();
        int targetSlotCount = settings.slotCount();

        List<HeroBanner> banners;
        if (cinemaId != null && cinemaId > 0) {
            Cinema cinema = cinemaRepository.findById(cinemaId).orElse(null);
            String city = cinema != null && cinema.getCity() != null ? cinema.getCity().trim() : "";
            banners = heroBannerRepository.findActivePublicBannersForCinema(BannerStatus.PUBLISHED, now, cinemaId, city);
        } else {
            banners = heroBannerRepository.findActivePublicBanners(BannerStatus.PUBLISHED, now);
        }

        // Filter out banners whose linked entities are invalid
        List<HeroBannerResponse> validResponses = banners.stream()
                .filter(this::isBannerContentEligible)
                .map(this::toResponse)
                .toList();

        // If Auto-Fill is disabled: return only manual banners up to targetSlotCount
        if (!settings.autoFillEnabled()) {
            List<HeroBannerResponse> limited = validResponses.stream().limit(targetSlotCount).toList();
            return limited.isEmpty() ? getStaticFallback() : limited;
        }

        // If Auto-Fill is enabled: fill empty slots up to targetSlotCount
        if (validResponses.size() < targetSlotCount) {
            int needed = targetSlotCount - validResponses.size();
            List<HeroBannerResponse> filledBanners = getFallbackBanners(needed, validResponses);
            List<HeroBannerResponse> combined = new ArrayList<>(validResponses);
            combined.addAll(filledBanners);
            return combined.isEmpty() ? getStaticFallback() : combined;
        }

        return validResponses.stream().limit(targetSlotCount).toList();
    }

    private boolean isBannerContentEligible(HeroBanner banner) {
        if (banner.getType() == BannerType.MOVIE && banner.getLinkedMovie() != null) {
            Movie movie = banner.getLinkedMovie();
            if (movie.getStatus() == MovieStatus.INACTIVE || movie.getStatus() == MovieStatus.ENDED) {
                return false;
            }
        }
        return true;
    }

    private List<HeroBannerResponse> getFallbackBanners(int needed, List<HeroBannerResponse> existingBanners) {
        if (needed <= 0) return List.of();

        Set<Long> existingMovieIds = existingBanners != null ? existingBanners.stream()
                .map(HeroBannerResponse::linkedMovieId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()) : Set.of();

        Set<String> existingTitles = existingBanners != null ? existingBanners.stream()
                .map(b -> b.title() != null ? b.title().toUpperCase().trim() : "")
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet()) : Set.of();

        List<Movie> latestMovies = movieRepository.findAll().stream()
                .filter(m -> m.getStatus() != MovieStatus.INACTIVE && m.getStatus() != MovieStatus.ENDED)
                .filter(m -> !existingMovieIds.contains(m.getId()) && !existingTitles.contains(m.getTitle().toUpperCase().trim()))
                .sorted((a, b) -> {
                    if (b.getCreatedAt() != null && a.getCreatedAt() != null) {
                        int cmp = b.getCreatedAt().compareTo(a.getCreatedAt());
                        if (cmp != 0) return cmp;
                    }
                    if (b.getReleaseDate() != null && a.getReleaseDate() != null) {
                        int cmp = b.getReleaseDate().compareTo(a.getReleaseDate());
                        if (cmp != 0) return cmp;
                    }
                    return Long.compare(b.getId() != null ? b.getId() : 0L, a.getId() != null ? a.getId() : 0L);
                })
                .limit(needed)
                .toList();

        int startIndex = existingBanners != null ? existingBanners.size() : 0;
        List<HeroBannerResponse> dynamicBanners = new ArrayList<>();
        for (int i = 0; i < latestMovies.size(); i++) {
            Movie movie = latestMovies.get(i);
            boolean isNowShowing = movie.getStatus() == MovieStatus.NOW_SHOWING;
            String backdrop = (movie.getAvatarUrl() != null && !movie.getAvatarUrl().isBlank())
                    ? movie.getAvatarUrl()
                    : (movie.getPosterUrl() != null && !movie.getPosterUrl().isBlank()
                            ? movie.getPosterUrl()
                            : "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1920&q=85");
            String mobileImg = (movie.getPosterUrl() != null && !movie.getPosterUrl().isBlank())
                    ? movie.getPosterUrl()
                    : backdrop;

            String badge = isNowShowing ? "BOM TẤN ĐANG CHIẾU" : "SẮP KHỞI CHIẾU";
            String ageRatingStr = movie.getAgeRating() != null ? movie.getAgeRating().name() : "P";
            String formatLabel = movie.getDurationMinutes() > 0
                    ? movie.getDurationMinutes() + " PHÚT • " + ageRatingStr + " • IMAX LASER"
                    : "IMAX LASER 70MM";

            String primaryCtaLabel = isNowShowing ? "ĐẶT VÉ NGAY" : "CHI TIẾT PHIM";
            String primaryCtaUrl = isNowShowing ? "/showtimes?movieId=" + movie.getId() : "/movies/" + movie.getId();
            boolean hasTrailer = movie.getTrailerUrl() != null && !movie.getTrailerUrl().isBlank();

            String subtitle = movie.getDescription() != null && !movie.getDescription().isBlank()
                    ? (movie.getDescription().length() > 120 ? movie.getDescription().substring(0, 120) + "..." : movie.getDescription())
                    : "Trải nghiệm đỉnh cao phòng vé CinePremier";

            dynamicBanners.add(new HeroBannerResponse(
                    -movie.getId(),
                    "Banner Tự Động: " + movie.getTitle(),
                    BannerType.MOVIE,
                    movie.getTitle().toUpperCase(),
                    subtitle,
                    movie.getDescription(),
                    badge,
                    formatLabel,
                    backdrop,
                    mobileImg,
                    movie.getTitle(),
                    BannerFocalPoint.CENTER,
                    backdrop,
                    mobileImg,
                    formatLabel,
                    movie.getId(),
                    movie.getId(),
                    movie.getTitle(),
                    movie.getPosterUrl(),
                    movie.getTrailerUrl(),
                    movie.getDurationMinutes(),
                    ageRatingStr,
                    movie.getStatus().name(),
                    null,
                    null,
                    null,
                    false,
                    null,
                    null,
                    BannerCtaType.BOOK_NOW,
                    primaryCtaLabel,
                    primaryCtaUrl,
                    hasTrailer ? BannerCtaType.INTERNAL_URL : BannerCtaType.NONE,
                    hasTrailer ? "XEM TRAILER" : null,
                    hasTrailer ? movie.getTrailerUrl() : null,
                    primaryCtaLabel,
                    primaryCtaUrl,
                    BannerScopeType.GLOBAL,
                    null,
                    List.of(),
                    10 - (startIndex + i),
                    startIndex + i + 1,
                    6,
                    null,
                    null,
                    BannerStatus.PUBLISHED,
                    0,
                    0,
                    0.0,
                    null,
                    "Hệ thống (Phim mới nhất)",
                    null,
                    null,
                    movie.getCreatedAt() != null ? movie.getCreatedAt() : LocalDateTime.now(),
                    movie.getCreatedAt() != null ? movie.getCreatedAt() : LocalDateTime.now(),
                    LocalDateTime.now()
            ));
        }
        return dynamicBanners;
    }

    private List<HeroBannerResponse> getStaticFallback() {
        return List.of(
                new HeroBannerResponse(
                        0L,
                        "CinePremier Default Experience",
                        BannerType.CUSTOM,
                        "CINEPREMIER CINEMAS",
                        "Trải nghiệm điện ảnh đỉnh cao với hệ thống phòng chiếu IMAX Laser & Dolby Atmos",
                        "Chào mừng quý khách đến với CinePremier.",
                        "PREMIER EXPERIENCE",
                        "IMAX LASER 70MM",
                        "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1920&q=85",
                        "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=780&q=85",
                        "CinePremier Cinema Showcase",
                        BannerFocalPoint.CENTER,
                        "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1920&q=85",
                        "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=780&q=85",
                        "IMAX LASER 70MM",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        BannerCtaType.BOOK_NOW,
                        "XEM LỊCH CHIẾU",
                        "/showtimes",
                        BannerCtaType.NONE,
                        null,
                        null,
                        "XEM LỊCH CHIẾU",
                        "/showtimes",
                        BannerScopeType.GLOBAL,
                        null,
                        List.of(),
                        1,
                        1,
                        6,
                        null,
                        null,
                        BannerStatus.PUBLISHED,
                        0,
                        0,
                        0.0,
                        null,
                        "System",
                        null,
                        null,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        LocalDateTime.now()
                )
        );
    }

    @Override
    @Transactional
    public void recordImpression(Long bannerId) {
        if (bannerId == null || bannerId <= 0) return;
        heroBannerRepository.findById(bannerId).ifPresent(HeroBanner::incrementImpression);
    }

    @Override
    @Transactional
    public void recordClick(Long bannerId) {
        if (bannerId == null || bannerId <= 0) return;
        heroBannerRepository.findById(bannerId).ifPresent(HeroBanner::incrementClick);
    }

    // ==========================================
    // 2. ADMIN API
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<HeroBannerResponse> getAdminBanners(
            String search,
            BannerStatus status,
            BannerType type,
            BannerScopeType scopeType,
            Long cinemaId,
            int page,
            int size,
            AuthenticatedUser currentUser
    ) {
        cinemaSecurityService.validateCinemaAccess(currentUser, cinemaId);

        Long effectiveCinemaId = cinemaSecurityService.resolveEnforcedCinemaId(currentUser, cinemaId);

        Specification<HeroBanner> spec = Specification.where(null);

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("subtitle")), pattern)
            ));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }

        if (scopeType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("scopeType"), scopeType));
        }

        if (effectiveCinemaId != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                var join = root.join("bannerCinemas", jakarta.persistence.criteria.JoinType.LEFT);
                return cb.or(
                        cb.equal(root.get("scopeType"), BannerScopeType.GLOBAL),
                        cb.equal(join.get("cinema").get("id"), effectiveCinemaId)
                );
            });
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.desc("priority"), Sort.Order.desc("id")));

        Page<HeroBanner> pagedResult = heroBannerRepository.findAll(spec, pageable);
        List<HeroBannerResponse> items = pagedResult.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(
                items,
                pagedResult.getNumber(),
                pagedResult.getSize(),
                pagedResult.getTotalElements(),
                pagedResult.getTotalPages(),
                pagedResult.isFirst(),
                pagedResult.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public HeroBannerResponse getAdminBannerById(Long id, AuthenticatedUser currentUser) {
        HeroBanner banner = heroBannerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Hero Banner với ID: " + id));

        validateUserAccessToBanner(banner, currentUser);
        return toResponse(banner);
    }

    @Override
    @Transactional
    public HeroBannerResponse createBanner(CreateHeroBannerRequest request, AuthenticatedUser currentUser) {
        validateBusinessRules(
                request.type(),
                request.linkedMovieId(),
                request.linkedPromotionId(),
                request.linkedFnbId(),
                request.primaryCtaType(),
                request.primaryCtaUrl(),
                request.secondaryCtaType(),
                request.secondaryCtaUrl(),
                request.desktopImageUrl(),
                request.mobileImageUrl(),
                request.scopeType(),
                request.targetCity(),
                request.cinemaIds(),
                request.startAt(),
                request.endAt()
        );

        validateManagerScopedCreation(request.scopeType(), request.cinemaIds(), currentUser);

        String title = (request.title() != null && !request.title().isBlank())
                ? request.title().trim()
                : (Boolean.TRUE.equals(request.isImageOnly()) ? "" : request.name().trim());

        HeroBanner banner = new HeroBanner(
                request.name().trim(),
                request.type(),
                title,
                request.subtitle() != null ? request.subtitle().trim() : null,
                request.desktopImageUrl().trim(),
                request.mobileImageUrl() != null ? request.mobileImageUrl().trim() : null,
                request.scopeType()
        );

        applyCommonFields(banner, request.description(), request.badge(), request.formatLabel(),
                request.altText(), request.focalPoint(), request.linkedMovieId(), request.linkedPromotionId(),
                request.isImageOnly(), request.linkedFnbId(), request.fnbType(),
                request.primaryCtaType(), request.primaryCtaLabel(), request.primaryCtaUrl(),
                request.secondaryCtaType(), request.secondaryCtaLabel(), request.secondaryCtaUrl(),
                request.targetCity(), request.priority(), request.sortOrder(), request.slideDurationSeconds(),
                request.startAt(), request.endAt());

        if (currentUser != null) {
            banner.setCreatedBy(currentUser.id());
            banner.setUpdatedBy(currentUser.id());
        }

        // Set sort order
        if (request.sortOrder() == null || request.sortOrder() <= 0) {
            banner.setSortOrder(heroBannerRepository.findMaxSortOrder() + 1);
        }

        // Publish immediately if requested
        if (Boolean.TRUE.equals(request.publishNow())) {
            assertCanPublish(banner);
            banner.setStatus(BannerStatus.PUBLISHED);
            banner.setPublishedAt(LocalDateTime.now());
        }

        HeroBanner savedBanner = heroBannerRepository.save(banner);
        syncCinemas(savedBanner, request.scopeType(), request.cinemaIds());

        auditLogService.record(
                AuditActionType.CREATE,
                "HeroBanner",
                savedBanner.getId(),
                "Tạo Hero Banner mới: " + savedBanner.getName() + " [Status: " + savedBanner.getStatus() + "]"
        );

        return toResponse(savedBanner);
    }

    @Override
    @Transactional
    public HeroBannerResponse updateBanner(Long id, UpdateHeroBannerRequest request, AuthenticatedUser currentUser) {
        HeroBanner banner = heroBannerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Hero Banner với ID: " + id));

        validateUserAccessToBanner(banner, currentUser);

        if (banner.getStatus() == BannerStatus.ARCHIVED) {
            throw new BadRequestException("Không thể chỉnh sửa Banner đã được lưu trữ (ARCHIVED). Vui lòng nhân bản hoặc tạo mới.");
        }

        validateBusinessRules(
                request.type(),
                request.linkedMovieId(),
                request.linkedPromotionId(),
                request.linkedFnbId(),
                request.primaryCtaType(),
                request.primaryCtaUrl(),
                request.secondaryCtaType(),
                request.secondaryCtaUrl(),
                request.desktopImageUrl(),
                request.mobileImageUrl(),
                request.scopeType(),
                request.targetCity(),
                request.cinemaIds(),
                request.startAt(),
                request.endAt()
        );

        validateManagerScopedCreation(request.scopeType(), request.cinemaIds(), currentUser);

        String title = (request.title() != null && !request.title().isBlank())
                ? request.title().trim()
                : (Boolean.TRUE.equals(request.isImageOnly()) ? "" : banner.getTitle());

        banner.setName(request.name().trim());
        banner.setType(request.type());
        banner.setTitle(title);
        banner.setSubtitle(request.subtitle() != null ? request.subtitle().trim() : null);
        banner.setDesktopImageUrl(request.desktopImageUrl().trim());
        banner.setMobileImageUrl(request.mobileImageUrl() != null ? request.mobileImageUrl().trim() : null);
        banner.setScopeType(request.scopeType());

        applyCommonFields(banner, request.description(), request.badge(), request.formatLabel(),
                request.altText(), request.focalPoint(), request.linkedMovieId(), request.linkedPromotionId(),
                request.isImageOnly(), request.linkedFnbId(), request.fnbType(),
                request.primaryCtaType(), request.primaryCtaLabel(), request.primaryCtaUrl(),
                request.secondaryCtaType(), request.secondaryCtaLabel(), request.secondaryCtaUrl(),
                request.targetCity(), request.priority(), request.sortOrder(), request.slideDurationSeconds(),
                request.startAt(), request.endAt());

        if (currentUser != null) {
            banner.setUpdatedBy(currentUser.id());
        }

        syncCinemas(banner, request.scopeType(), request.cinemaIds());
        HeroBanner updatedBanner = heroBannerRepository.save(banner);

        auditLogService.record(
                AuditActionType.UPDATE,
                "HeroBanner",
                updatedBanner.getId(),
                "Cập nhật Hero Banner: " + updatedBanner.getName()
        );

        return toResponse(updatedBanner);
    }

    @Override
    @Transactional
    public HeroBannerResponse publishBanner(Long id, AuthenticatedUser currentUser) {
        HeroBanner banner = heroBannerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Hero Banner với ID: " + id));

        validateUserAccessToBanner(banner, currentUser);

        if (banner.getStatus() == BannerStatus.PUBLISHED) {
            return toResponse(banner);
        }

        assertCanPublish(banner);

        banner.setStatus(BannerStatus.PUBLISHED);
        banner.setPublishedAt(LocalDateTime.now());
        if (currentUser != null) {
            banner.setUpdatedBy(currentUser.id());
        }

        HeroBanner saved = heroBannerRepository.save(banner);
        auditLogService.record(AuditActionType.UPDATE, "HeroBanner", saved.getId(), "Xuất bản Hero Banner: " + saved.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public HeroBannerResponse pauseBanner(Long id, AuthenticatedUser currentUser) {
        HeroBanner banner = heroBannerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Hero Banner với ID: " + id));

        validateUserAccessToBanner(banner, currentUser);

        if (banner.getStatus() != BannerStatus.PUBLISHED) {
            throw new BadRequestException("Chỉ có thể tạm dừng (PAUSE) banner đang ở trạng thái PUBLISHED");
        }

        banner.setStatus(BannerStatus.PAUSED);
        if (currentUser != null) {
            banner.setUpdatedBy(currentUser.id());
        }

        HeroBanner saved = heroBannerRepository.save(banner);
        auditLogService.record(AuditActionType.UPDATE, "HeroBanner", saved.getId(), "Tạm dừng Hero Banner: " + saved.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public HeroBannerResponse resumeBanner(Long id, AuthenticatedUser currentUser) {
        HeroBanner banner = heroBannerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Hero Banner với ID: " + id));

        validateUserAccessToBanner(banner, currentUser);

        if (banner.getStatus() != BannerStatus.PAUSED) {
            throw new BadRequestException("Chỉ có thể tiếp tục chạy (RESUME) banner đang bị tạm dừng (PAUSED)");
        }

        assertCanPublish(banner);

        banner.setStatus(BannerStatus.PUBLISHED);
        if (currentUser != null) {
            banner.setUpdatedBy(currentUser.id());
        }

        HeroBanner saved = heroBannerRepository.save(banner);
        auditLogService.record(AuditActionType.UPDATE, "HeroBanner", saved.getId(), "Tiếp tục chạy Hero Banner: " + saved.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public HeroBannerResponse archiveBanner(Long id, AuthenticatedUser currentUser) {
        HeroBanner banner = heroBannerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Hero Banner với ID: " + id));

        validateUserAccessToBanner(banner, currentUser);

        banner.setStatus(BannerStatus.ARCHIVED);
        if (currentUser != null) {
            banner.setUpdatedBy(currentUser.id());
        }

        HeroBanner saved = heroBannerRepository.save(banner);
        auditLogService.record(AuditActionType.UPDATE, "HeroBanner", saved.getId(), "Lưu trữ Hero Banner: " + saved.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public HeroBannerResponse duplicateBanner(Long id, AuthenticatedUser currentUser) {
        HeroBanner original = heroBannerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Hero Banner với ID: " + id));

        validateUserAccessToBanner(original, currentUser);

        HeroBanner clone = new HeroBanner(
                original.getName() + " (Copy)",
                original.getType(),
                original.getTitle(),
                original.getSubtitle(),
                original.getDesktopImageUrl(),
                original.getMobileImageUrl(),
                original.getScopeType()
        );

        clone.setDescription(original.getDescription());
        clone.setBadge(original.getBadge());
        clone.setFormatLabel(original.getFormatLabel());
        clone.setAltText(original.getAltText());
        clone.setFocalPoint(original.getFocalPoint());
        clone.setLinkedMovie(original.getLinkedMovie());
        clone.setLinkedPromotionId(original.getLinkedPromotionId());
        clone.setPrimaryCtaType(original.getPrimaryCtaType());
        clone.setPrimaryCtaLabel(original.getPrimaryCtaLabel());
        clone.setPrimaryCtaUrl(original.getPrimaryCtaUrl());
        clone.setSecondaryCtaType(original.getSecondaryCtaType());
        clone.setSecondaryCtaLabel(original.getSecondaryCtaLabel());
        clone.setSecondaryCtaUrl(original.getSecondaryCtaUrl());
        clone.setTargetCity(original.getTargetCity());
        clone.setPriority(original.getPriority());
        clone.setSortOrder(heroBannerRepository.findMaxSortOrder() + 1);
        clone.setSlideDurationSeconds(original.getSlideDurationSeconds());
        clone.setStatus(BannerStatus.DRAFT);

        if (currentUser != null) {
            clone.setCreatedBy(currentUser.id());
            clone.setUpdatedBy(currentUser.id());
        }

        HeroBanner savedClone = heroBannerRepository.save(clone);

        if (original.getScopeType() == BannerScopeType.CINEMA && !original.getBannerCinemas().isEmpty()) {
            List<Long> cinemaIds = original.getBannerCinemas().stream()
                    .map(bc -> bc.getCinema().getId())
                    .toList();
            syncCinemas(savedClone, BannerScopeType.CINEMA, cinemaIds);
        }

        auditLogService.record(AuditActionType.CREATE, "HeroBanner", savedClone.getId(), "Nhân bản từ Hero Banner ID: " + id);
        return toResponse(savedClone);
    }

    @Override
    @Transactional
    public void reorderBanners(List<Long> bannerIds, AuthenticatedUser currentUser) {
        if (bannerIds == null || bannerIds.isEmpty()) return;

        for (int i = 0; i < bannerIds.size(); i++) {
            Long bannerId = bannerIds.get(i);
            int newOrder = i + 1;
            heroBannerRepository.findById(bannerId).ifPresent(b -> {
                validateUserAccessToBanner(b, currentUser);
                b.setSortOrder(newOrder);
                if (currentUser != null) {
                    b.setUpdatedBy(currentUser.id());
                }
            });
        }

        auditLogService.record(AuditActionType.UPDATE, "HeroBanner", null, "Sắp xếp lại thứ tự của " + bannerIds.size() + " Hero Banners");
    }

    @Override
    @Transactional
    public void deleteDraftBanner(Long id, AuthenticatedUser currentUser) {
        HeroBanner banner = heroBannerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Hero Banner với ID: " + id));

        validateUserAccessToBanner(banner, currentUser);

        if (banner.getStatus() != BannerStatus.DRAFT) {
            throw new BadRequestException("Chỉ có thể xóa vĩnh viễn banner ở trạng thái DRAFT. Với các trạng thái khác, vui lòng dùng ARCHIVED.");
        }

        heroBannerCinemaRepository.deleteByHeroBannerId(banner.getId());
        heroBannerRepository.delete(banner);

        auditLogService.record(AuditActionType.DELETE, "HeroBanner", id, "Xóa bản nháp Hero Banner: " + banner.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getBannerSummaryStats() {
        LocalDateTime now = LocalDateTime.now();

        long totalBanners = heroBannerRepository.count();
        long publishedBanners = heroBannerRepository.countByStatus(BannerStatus.PUBLISHED);
        long activeBanners = heroBannerRepository.countCurrentlyActive(BannerStatus.PUBLISHED, now);
        long scheduledBanners = heroBannerRepository.countByStatus(BannerStatus.SCHEDULED);
        long draftBanners = heroBannerRepository.countByStatus(BannerStatus.DRAFT);
        long pausedBanners = heroBannerRepository.countByStatus(BannerStatus.PAUSED);
        long archivedBanners = heroBannerRepository.countByStatus(BannerStatus.ARCHIVED);

        List<HeroBanner> all = heroBannerRepository.findAll();
        long totalImpressions = all.stream().mapToLong(HeroBanner::getImpressionsCount).sum();
        long totalClicks = all.stream().mapToLong(HeroBanner::getClicksCount).sum();
        double overallCtr = totalImpressions > 0 ? ((double) totalClicks / totalImpressions) * 100.0 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", totalBanners);
        stats.put("published", publishedBanners);
        stats.put("active", activeBanners);
        stats.put("scheduled", scheduledBanners);
        stats.put("draft", draftBanners);
        stats.put("paused", pausedBanners);
        stats.put("archived", archivedBanners);
        stats.put("totalImpressions", totalImpressions);
        stats.put("totalClicks", totalClicks);
        stats.put("overallCtr", Math.round(overallCtr * 100.0) / 100.0);
        stats.put("maxActiveLimit", maxActiveBanners);

        return stats;
    }

    // ==========================================
    // 2.1 HERO SLOT MANAGEMENT & AUTO-FILL
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public HeroSlotSettingsDto getSlotSettings() {
        int slotCount = 5;
        boolean autoFill = true;
        boolean nowShowing = true;
        boolean comingSoon = false;
        int refreshMinutes = 15;

        try {
            var sc = systemSettingRepository.findByConfigKey("hero.carousel.slots.count");
            if (sc.isPresent()) {
                slotCount = Math.max(1, Math.min(10, Integer.parseInt(sc.get().getConfigValue().trim())));
            }
            var af = systemSettingRepository.findByConfigKey("hero.carousel.autofill.enabled");
            if (af.isPresent()) {
                autoFill = Boolean.parseBoolean(af.get().getConfigValue().trim());
            }
            var ns = systemSettingRepository.findByConfigKey("hero.carousel.autofill.source.now_showing");
            if (ns.isPresent()) {
                nowShowing = Boolean.parseBoolean(ns.get().getConfigValue().trim());
            }
            var cs = systemSettingRepository.findByConfigKey("hero.carousel.autofill.source.coming_soon");
            if (cs.isPresent()) {
                comingSoon = Boolean.parseBoolean(cs.get().getConfigValue().trim());
            }
            var rm = systemSettingRepository.findByConfigKey("hero.carousel.autofill.refresh_minutes");
            if (rm.isPresent()) {
                refreshMinutes = Math.max(5, Integer.parseInt(rm.get().getConfigValue().trim()));
            }
        } catch (Exception e) {
            log.warn("Error reading hero carousel settings, using defaults: {}", e.getMessage());
        }

        return new HeroSlotSettingsDto(slotCount, autoFill, nowShowing, comingSoon, refreshMinutes);
    }

    @Override
    @Transactional
    public HeroSlotSettingsDto updateSlotSettings(HeroSlotSettingsDto request, AuthenticatedUser currentUser) {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new ForbiddenException("Chỉ Quản trị viên (ADMIN) mới có quyền thay đổi cài đặt Hero Carousel");
        }

        int slotCount = Math.max(1, Math.min(10, request.slotCount()));
        String actor = currentUser != null && currentUser.email() != null ? currentUser.email() : "ADMIN";
        upsertSetting("hero.carousel.slots.count", String.valueOf(slotCount), "Số lượng Hero Slot hiển thị", actor);
        upsertSetting("hero.carousel.autofill.enabled", String.valueOf(request.autoFillEnabled()), "Tự động lấp đầy Hero Carousel bằng phim", actor);
        upsertSetting("hero.carousel.autofill.source.now_showing", String.valueOf(request.autoSourceNowShowing()), "Auto-fill từ phim đang chiếu", actor);
        upsertSetting("hero.carousel.autofill.source.coming_soon", String.valueOf(request.autoSourceComingSoon()), "Auto-fill từ phim sắp chiếu", actor);
        upsertSetting("hero.carousel.autofill.refresh_minutes", String.valueOf(Math.max(5, request.refreshMinutes())), "Chu kỳ refresh auto-fill (phút)", actor);

        auditLogService.record(
                AuditActionType.UPDATE,
                "HeroSlotSettings",
                0L,
                "Cập nhật cài đặt Hero Carousel: " + slotCount + " slots, Auto-fill=" + request.autoFillEnabled()
        );

        return new HeroSlotSettingsDto(slotCount, request.autoFillEnabled(), request.autoSourceNowShowing(), request.autoSourceComingSoon(), Math.max(5, request.refreshMinutes()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HeroSlotDto> getHeroSlots(Long cinemaId) {
        HeroSlotSettingsDto settings = getSlotSettings();
        int totalSlots = settings.slotCount();
        LocalDateTime now = LocalDateTime.now();

        List<HeroBanner> activeBanners;
        if (cinemaId != null && cinemaId > 0) {
            Cinema cinema = cinemaRepository.findById(cinemaId).orElse(null);
            String city = cinema != null && cinema.getCity() != null ? cinema.getCity().trim() : "";
            activeBanners = heroBannerRepository.findActivePublicBannersForCinema(BannerStatus.PUBLISHED, now, cinemaId, city);
        } else {
            activeBanners = heroBannerRepository.findActivePublicBanners(BannerStatus.PUBLISHED, now);
        }

        List<HeroBanner> manualBanners = activeBanners.stream()
                .filter(this::isBannerContentEligible)
                .toList();

        Set<Long> manualMovieIds = manualBanners.stream()
                .map(b -> b.getLinkedMovie() != null ? b.getLinkedMovie().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> assignedMovieIds = new HashSet<>(manualMovieIds);

        List<Movie> candidateMovies = new ArrayList<>();
        if (settings.autoFillEnabled()) {
            candidateMovies = movieRepository.findAll().stream()
                    .filter(m -> {
                        if (m.getStatus() == MovieStatus.INACTIVE || m.getStatus() == MovieStatus.ENDED) return false;
                        if (!settings.autoSourceComingSoon() && m.getStatus() == MovieStatus.UPCOMING) return false;
                        if (!settings.autoSourceNowShowing() && m.getStatus() == MovieStatus.NOW_SHOWING) return false;
                        return true;
                    })
                    .sorted((a, b) -> {
                        if (b.getCreatedAt() != null && a.getCreatedAt() != null) {
                            int cmp = b.getCreatedAt().compareTo(a.getCreatedAt());
                            if (cmp != 0) return cmp;
                        }
                        if (b.getReleaseDate() != null && a.getReleaseDate() != null) {
                            int cmp = b.getReleaseDate().compareTo(a.getReleaseDate());
                            if (cmp != 0) return cmp;
                        }
                        return Long.compare(b.getId() != null ? b.getId() : 0L, a.getId() != null ? a.getId() : 0L);
                    })
                    .toList();
        }

        List<HeroSlotDto> slots = new ArrayList<>();
        int manualIndex = 0;
        int autoMovieIndex = 0;

        for (int pos = 1; pos <= totalSlots; pos++) {
            if (manualIndex < manualBanners.size()) {
                HeroBanner banner = manualBanners.get(manualIndex++);
                slots.add(new HeroSlotDto(pos, "MANUAL", true, toResponse(banner), null));
            } else if (settings.autoFillEnabled()) {
                Movie autoMovie = null;
                while (autoMovieIndex < candidateMovies.size()) {
                    Movie cand = candidateMovies.get(autoMovieIndex++);
                    if (!assignedMovieIds.contains(cand.getId())) {
                        autoMovie = cand;
                        assignedMovieIds.add(cand.getId());
                        break;
                    }
                }

                if (autoMovie != null) {
                    Map<String, Object> movieMap = toAutoMovieMap(autoMovie);
                    slots.add(new HeroSlotDto(pos, "AUTO", true, null, movieMap));
                } else {
                    slots.add(new HeroSlotDto(pos, "EMPTY", false, null, null));
                }
            } else {
                slots.add(new HeroSlotDto(pos, "EMPTY", false, null, null));
            }
        }

        return slots;
    }

    @Override
    @Transactional
    public HeroBannerResponse pinAutoSlot(int position, Long cinemaId, AuthenticatedUser currentUser) {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new ForbiddenException("Chỉ Quản trị viên (ADMIN) mới có quyền Ghim banner tự động");
        }

        List<HeroSlotDto> slots = getHeroSlots(cinemaId);
        HeroSlotDto targetSlot = slots.stream()
                .filter(s -> s.position() == position)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Không tìm thấy slot ở vị trí " + position));

        if (!"AUTO".equalsIgnoreCase(targetSlot.assignmentType()) || targetSlot.autoMovie() == null) {
            throw new BadRequestException("Slot tại vị trí " + position + " không phải là phim tự động (AUTO)");
        }

        Number movieIdNum = (Number) targetSlot.autoMovie().get("movieId");
        if (movieIdNum == null) {
            throw new BadRequestException("Không tìm thấy ID phim của slot tự động");
        }
        Long movieId = movieIdNum.longValue();
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phim với ID: " + movieId));

        String backdrop = (movie.getAvatarUrl() != null && !movie.getAvatarUrl().isBlank())
                ? movie.getAvatarUrl()
                : (movie.getPosterUrl() != null ? movie.getPosterUrl() : "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1920&q=85");
        String mobileImg = (movie.getPosterUrl() != null && !movie.getPosterUrl().isBlank()) ? movie.getPosterUrl() : backdrop;

        HeroBanner banner = new HeroBanner(
                movie.getTitle() + " - Hero",
                BannerType.MOVIE,
                movie.getTitle().toUpperCase(),
                movie.getDescription() != null && !movie.getDescription().isBlank()
                        ? (movie.getDescription().length() > 120 ? movie.getDescription().substring(0, 120) + "..." : movie.getDescription())
                        : "Trải nghiệm đỉnh cao phòng vé CinePremier",
                backdrop,
                mobileImg,
                BannerScopeType.GLOBAL
        );

        banner.setLinkedMovie(movie);
        banner.setBadge(movie.getStatus() == MovieStatus.NOW_SHOWING ? "BOM TẤN ĐANG CHIẾU" : "SẮP KHỞI CHIẾU");
        banner.setFormatLabel(movie.getDurationMinutes() > 0 ? movie.getDurationMinutes() + " PHÚT • IMAX LASER" : "IMAX LASER 70MM");
        banner.setPrimaryCtaType(BannerCtaType.BOOK_NOW);
        banner.setPrimaryCtaLabel("ĐẶT VÉ NGAY");
        banner.setPrimaryCtaUrl("/showtimes?movieId=" + movie.getId());
        if (movie.getTrailerUrl() != null && !movie.getTrailerUrl().isBlank()) {
            banner.setSecondaryCtaType(BannerCtaType.INTERNAL_URL);
            banner.setSecondaryCtaLabel("XEM TRAILER");
            banner.setSecondaryCtaUrl(movie.getTrailerUrl());
        }
        banner.setPriority(1);
        banner.setSortOrder(position);
        banner.setSlideDurationSeconds(DEFAULT_DURATION);
        banner.setStatus(BannerStatus.PUBLISHED);
        banner.setPublishedAt(LocalDateTime.now());
        banner.setCreatedBy(currentUser.id());
        banner.setUpdatedBy(currentUser.id());

        HeroBanner saved = heroBannerRepository.save(banner);
        auditLogService.record(
                AuditActionType.CREATE,
                "HeroBanner",
                saved.getId(),
                "Ghim phim tự động vào Hero Carousel: " + saved.getName() + " (Slot " + position + ")"
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void unassignSlot(int position, AuthenticatedUser currentUser) {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new ForbiddenException("Chỉ Quản trị viên (ADMIN) mới có quyền gỡ banner khỏi slot");
        }

        List<HeroSlotDto> slots = getHeroSlots(null);
        HeroSlotDto targetSlot = slots.stream()
                .filter(s -> s.position() == position)
                .findFirst()
                .orElse(null);

        if (targetSlot != null && "MANUAL".equalsIgnoreCase(targetSlot.assignmentType()) && targetSlot.banner() != null) {
            HeroBanner banner = heroBannerRepository.findById(targetSlot.banner().id()).orElse(null);
            if (banner != null) {
                banner.setStatus(BannerStatus.PAUSED);
                banner.setUpdatedBy(currentUser.id());
                heroBannerRepository.save(banner);

                auditLogService.record(
                        AuditActionType.UPDATE,
                        "HeroBanner",
                        banner.getId(),
                        "Gỡ banner khỏi Hero Carousel (Slot " + position + ") chuyển trạng thái PAUSED"
                );
            }
        }
    }

    @Override
    @Transactional
    public void assignBannerToSlot(int position, Long bannerId, AuthenticatedUser currentUser) {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new ForbiddenException("Chỉ Quản trị viên (ADMIN) mới có quyền gán banner vào slot");
        }

        HeroBanner banner = heroBannerRepository.findById(bannerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Hero Banner với ID: " + bannerId));

        banner.setSortOrder(position);
        if (banner.getStatus() != BannerStatus.PUBLISHED) {
            banner.setStatus(BannerStatus.PUBLISHED);
            banner.setPublishedAt(LocalDateTime.now());
        }
        banner.setUpdatedBy(currentUser.id());
        heroBannerRepository.save(banner);

        auditLogService.record(
                AuditActionType.UPDATE,
                "HeroBanner",
                banner.getId(),
                "Gán banner vào Slot " + position + ": " + banner.getName()
        );
    }

    private void upsertSetting(String key, String value, String description, String username) {
        SystemSetting setting = systemSettingRepository.findByConfigKey(key)
                .orElse(new SystemSetting(key, value, description, username));
        setting.setConfigValue(value);
        setting.setUpdatedBy(username);
        systemSettingRepository.save(setting);
    }

    private Map<String, Object> toAutoMovieMap(Movie movie) {
        Map<String, Object> map = new HashMap<>();
        map.put("movieId", movie.getId());
        map.put("title", movie.getTitle());
        map.put("posterUrl", movie.getPosterUrl());
        map.put("avatarUrl", movie.getAvatarUrl());
        String backdrop = (movie.getAvatarUrl() != null && !movie.getAvatarUrl().isBlank())
                ? movie.getAvatarUrl()
                : (movie.getPosterUrl() != null ? movie.getPosterUrl() : "");
        map.put("backdropUrl", backdrop);
        map.put("trailerUrl", movie.getTrailerUrl());
        map.put("durationMinutes", movie.getDurationMinutes());
        map.put("ageRating", movie.getAgeRating() != null ? movie.getAgeRating().name() : "P");
        map.put("status", movie.getStatus() != null ? movie.getStatus().name() : "NOW_SHOWING");
        map.put("genre", "Điện ảnh");
        map.put("badge", movie.getStatus() == MovieStatus.NOW_SHOWING ? "PHIM HOT ĐANG CHIẾU" : "SẮP KHỞI CHIẾU");
        map.put("primaryCtaLabel", "ĐẶT VÉ NGAY");
        map.put("primaryCtaUrl", "/showtimes?movieId=" + movie.getId());
        return map;
    }

    // ==========================================
    // 3. PRIVATE HELPER METHODS & VALIDATIONS
    // ==========================================

    private void applyCommonFields(
            HeroBanner banner,
            String description,
            String badge,
            String formatLabel,
            String altText,
            BannerFocalPoint focalPoint,
            Long linkedMovieId,
            Long linkedPromotionId,
            Boolean isImageOnly,
            Long linkedFnbId,
            String fnbType,
            BannerCtaType primaryCtaType,
            String primaryCtaLabel,
            String primaryCtaUrl,
            BannerCtaType secondaryCtaType,
            String secondaryCtaLabel,
            String secondaryCtaUrl,
            String targetCity,
            Integer priority,
            Integer sortOrder,
            Integer slideDurationSeconds,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        banner.setDescription(description != null ? description.trim() : null);
        banner.setBadge(badge != null ? badge.trim() : null);
        banner.setFormatLabel(formatLabel != null ? formatLabel.trim() : null);
        banner.setAltText(altText != null ? altText.trim() : banner.getTitle());
        banner.setFocalPoint(focalPoint != null ? focalPoint : BannerFocalPoint.CENTER);

        if (linkedMovieId != null && linkedMovieId > 0) {
            Movie movie = movieRepository.findById(linkedMovieId)
                    .orElseThrow(() -> new NotFoundException("Phim liên kết không tồn tại với ID: " + linkedMovieId));
            banner.setLinkedMovie(movie);
        } else {
            banner.setLinkedMovie(null);
        }

        banner.setLinkedPromotionId(linkedPromotionId != null && linkedPromotionId > 0 ? linkedPromotionId : null);
        banner.setIsImageOnly(Boolean.TRUE.equals(isImageOnly));
        banner.setLinkedFnbId(linkedFnbId);
        banner.setFnbType(fnbType != null ? fnbType.trim() : null);

        banner.setPrimaryCtaType(primaryCtaType != null ? primaryCtaType : BannerCtaType.BOOK_NOW);
        banner.setPrimaryCtaLabel(primaryCtaLabel != null ? primaryCtaLabel.trim() : "ĐẶT VÉ NGAY");
        banner.setPrimaryCtaUrl(primaryCtaUrl != null ? primaryCtaUrl.trim() : null);

        banner.setSecondaryCtaType(secondaryCtaType != null ? secondaryCtaType : BannerCtaType.NONE);
        banner.setSecondaryCtaLabel(secondaryCtaLabel != null ? secondaryCtaLabel.trim() : null);
        banner.setSecondaryCtaUrl(secondaryCtaUrl != null ? secondaryCtaUrl.trim() : null);

        banner.setTargetCity(targetCity != null ? targetCity.trim() : null);
        banner.setPriority(priority != null && priority > 0 ? priority : 1);
        if (sortOrder != null && sortOrder > 0) {
            banner.setSortOrder(sortOrder);
        }

        int duration = slideDurationSeconds != null ? slideDurationSeconds : DEFAULT_DURATION;
        banner.setSlideDurationSeconds(Math.max(MIN_DURATION, Math.min(MAX_DURATION, duration)));

        banner.setStartAt(startAt);
        banner.setEndAt(endAt);
    }

    private void syncCinemas(HeroBanner banner, BannerScopeType scopeType, List<Long> cinemaIds) {
        banner.getBannerCinemas().clear();
        if (scopeType == BannerScopeType.CINEMA && cinemaIds != null && !cinemaIds.isEmpty()) {
            Set<Long> uniqueIds = cinemaIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
            for (Long cId : uniqueIds) {
                Cinema cinema = cinemaRepository.findById(cId)
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy Cụm rạp với ID: " + cId));
                banner.getBannerCinemas().add(new HeroBannerCinema(banner, cinema));
            }
        }
    }

    private void validateBusinessRules(
            BannerType type,
            Long linkedMovieId,
            Long linkedPromotionId,
            Long linkedFnbId,
            BannerCtaType primaryCtaType,
            String primaryCtaUrl,
            BannerCtaType secondaryCtaType,
            String secondaryCtaUrl,
            String desktopImageUrl,
            String mobileImageUrl,
            BannerScopeType scopeType,
            String targetCity,
            List<Long> cinemaIds,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        if (desktopImageUrl == null || !isValidUrl(desktopImageUrl)) {
            throw new BadRequestException("Đường dẫn ảnh desktop không hợp lệ: " + desktopImageUrl);
        }
        if (mobileImageUrl != null && !mobileImageUrl.isBlank() && !isValidUrl(mobileImageUrl)) {
            throw new BadRequestException("Đường dẫn ảnh mobile không hợp lệ: " + mobileImageUrl);
        }

        if (type == BannerType.MOVIE) {
            if (linkedMovieId == null || linkedMovieId <= 0) {
                throw new BadRequestException("Banner loại MOVIE bắt buộc phải chọn một phim liên kết");
            }
        }

        if (type == BannerType.FNB) {
            if (linkedFnbId == null || linkedFnbId <= 0) {
                throw new BadRequestException("Banner loại BẮP NƯỚC & F&B bắt buộc phải chọn một sản phẩm hoặc combo liên kết");
            }
        }

        if (primaryCtaType == BannerCtaType.EXTERNAL_URL) {
            if (primaryCtaUrl == null || !primaryCtaUrl.startsWith("http")) {
                throw new BadRequestException("Nút CTA chính loại EXTERNAL_URL yêu cầu URL bắt đầu bằng http:// hoặc https://");
            }
        }

        if (secondaryCtaType != null && secondaryCtaType == BannerCtaType.EXTERNAL_URL) {
            if (secondaryCtaUrl == null || !secondaryCtaUrl.startsWith("http")) {
                throw new BadRequestException("Nút CTA phụ loại EXTERNAL_URL yêu cầu URL bắt đầu bằng http:// hoặc https://");
            }
        }

        if (scopeType == BannerScopeType.CITY) {
            if (targetCity == null || targetCity.isBlank()) {
                throw new BadRequestException("Phạm vi CITY yêu cầu phải chỉ định tên thành phố mục tiêu");
            }
        } else if (scopeType == BannerScopeType.CINEMA) {
            if (cinemaIds == null || cinemaIds.isEmpty()) {
                throw new BadRequestException("Phạm vi CINEMA yêu cầu phải chọn ít nhất một cụm rạp áp dụng");
            }
        }

        if (startAt != null && endAt != null && !endAt.isAfter(startAt)) {
            throw new BadRequestException("Thời điểm kết thúc chiến dịch (endAt) phải diễn ra sau thời điểm bắt đầu (startAt)");
        }
    }

    private void assertCanPublish(HeroBanner banner) {
        if (banner.getStatus() == BannerStatus.ARCHIVED) {
            throw new BadRequestException("Banner đã lưu trữ (ARCHIVED) không thể xuất bản trực tiếp");
        }

        long activeCount = heroBannerRepository.countCurrentlyActive(BannerStatus.PUBLISHED, LocalDateTime.now());
        if (activeCount >= maxActiveBanners && banner.getStatus() != BannerStatus.PUBLISHED) {
            log.warn("Đã đạt giới hạn tối đa {} banner active đồng thời.", maxActiveBanners);
        }
    }

    private void validateUserAccessToBanner(HeroBanner banner, AuthenticatedUser currentUser) {
        if (currentUser == null) return;
        if (currentUser.isAdmin()) return;

        if (currentUser.isManager()) {
            Long managerCinemaId = currentUser.cinemaId();
            if (managerCinemaId == null) {
                throw new ForbiddenException("Quản lý chưa được phân công chi nhánh cụ thể");
            }
            if (banner.getScopeType() == BannerScopeType.CINEMA) {
                boolean matches = banner.getBannerCinemas().stream()
                        .anyMatch(bc -> bc.getCinema().getId().equals(managerCinemaId));
                if (!matches) {
                    throw new ForbiddenException("Quản lý không có quyền truy cập banner thuộc chi nhánh rạp khác");
                }
            }
            return;
        }

        throw new ForbiddenException("Bạn không có quyền thực hiện thao tác trên Banner này");
    }

    private void validateManagerScopedCreation(BannerScopeType scopeType, List<Long> cinemaIds, AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.isAdmin()) return;

        if (currentUser.isManager()) {
            Long managerCinemaId = currentUser.cinemaId();
            if (managerCinemaId == null) {
                throw new ForbiddenException("Quản lý chưa được phân công chi nhánh cụ thể để tạo banner");
            }
            if (scopeType != BannerScopeType.CINEMA) {
                throw new ForbiddenException("Quản lý chi nhánh chỉ được phép tạo banner thuộc phạm vi CINEMA");
            }
            if (cinemaIds == null || cinemaIds.size() != 1 || !cinemaIds.contains(managerCinemaId)) {
                throw new ForbiddenException("Quản lý chỉ được phép tạo banner cho chính chi nhánh được phân công (ID: " + managerCinemaId + ")");
            }
        }
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            URI uri = URI.create(url.trim());
            return uri.getScheme() != null || url.startsWith("/") || url.startsWith("data:image");
        } catch (Exception e) {
            return false;
        }
    }

    private HeroBannerResponse toResponse(HeroBanner banner) {
        Movie movie = banner.getLinkedMovie();
        double ctr = banner.getImpressionsCount() > 0
                ? ((double) banner.getClicksCount() / banner.getImpressionsCount()) * 100.0
                : 0.0;

        List<BannerCinemaSummary> cinemas = banner.getBannerCinemas().stream()
                .map(bc -> new BannerCinemaSummary(bc.getCinema().getId(), bc.getCinema().getName(), bc.getCinema().getCity()))
                .toList();

        String movieTitle = movie != null ? movie.getTitle() : null;
        String moviePoster = movie != null ? movie.getPosterUrl() : null;
        String movieTrailer = movie != null ? movie.getTrailerUrl() : null;
        Integer movieDuration = movie != null ? movie.getDurationMinutes() : null;
        String movieAgeRating = movie != null && movie.getAgeRating() != null ? movie.getAgeRating().name() : null;
        String movieStatus = movie != null ? movie.getStatus().name() : null;

        return new HeroBannerResponse(
                banner.getId(),
                banner.getName(),
                banner.getType(),
                banner.getTitle(),
                banner.getSubtitle(),
                banner.getDescription(),
                banner.getBadge(),
                banner.getFormatLabel(),
                banner.getDesktopImageUrl(),
                banner.getMobileImageUrl(),
                banner.getAltText(),
                banner.getFocalPoint(),
                banner.getDesktopImageUrl(),
                banner.getMobileImageUrl(),
                banner.getFormatLabel(),
                movie != null ? movie.getId() : null,
                movie != null ? movie.getId() : null,
                movieTitle,
                moviePoster,
                movieTrailer,
                movieDuration,
                movieAgeRating,
                movieStatus,
                banner.getLinkedPromotionId(),
                null,
                null,
                banner.getIsImageOnly(),
                banner.getLinkedFnbId(),
                banner.getFnbType(),
                banner.getPrimaryCtaType(),
                banner.getPrimaryCtaLabel(),
                banner.getPrimaryCtaUrl(),
                banner.getSecondaryCtaType(),
                banner.getSecondaryCtaLabel(),
                banner.getSecondaryCtaUrl(),
                banner.getPrimaryCtaLabel(),
                banner.getPrimaryCtaUrl(),
                banner.getScopeType(),
                banner.getTargetCity(),
                cinemas,
                banner.getPriority(),
                banner.getSortOrder(),
                banner.getSlideDurationSeconds(),
                banner.getStartAt(),
                banner.getEndAt(),
                banner.getStatus(),
                banner.getImpressionsCount(),
                banner.getClicksCount(),
                Math.round(ctr * 100.0) / 100.0,
                banner.getCreatedBy(),
                banner.getCreatedBy() != null ? "User #" + banner.getCreatedBy() : "Admin",
                banner.getUpdatedBy(),
                banner.getUpdatedBy() != null ? "User #" + banner.getUpdatedBy() : "Admin",
                banner.getPublishedAt(),
                banner.getCreatedAt(),
                banner.getUpdatedAt()
        );
    }
}
