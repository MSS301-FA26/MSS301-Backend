package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.banner.CreateHeroBannerRequest;
import com.sba301.cinemaai.dto.request.banner.UpdateHeroBannerRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.banner.HeroBannerResponse;
import com.sba301.cinemaai.dto.response.banner.HeroBannerResponse.BannerCinemaSummary;
import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.entity.HeroBanner;
import com.sba301.cinemaai.entity.HeroBannerCinema;
import com.sba301.cinemaai.entity.ManagerCinemaAssignment;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.Promotion;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.BannerCtaType;
import com.sba301.cinemaai.enums.BannerFocalPoint;
import com.sba301.cinemaai.enums.BannerScopeType;
import com.sba301.cinemaai.enums.BannerStatus;
import com.sba301.cinemaai.enums.BannerType;
import com.sba301.cinemaai.enums.MoviePublicationStatus;
import com.sba301.cinemaai.enums.MovieStatus;
import com.sba301.cinemaai.enums.PromotionStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.ForbiddenException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.CinemaRepository;
import com.sba301.cinemaai.repository.HeroBannerCinemaRepository;
import com.sba301.cinemaai.repository.HeroBannerRepository;
import com.sba301.cinemaai.repository.ManagerCinemaAssignmentRepository;
import com.sba301.cinemaai.repository.MovieRepository;
import com.sba301.cinemaai.repository.PromotionRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.HeroBannerService;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final PromotionRepository promotionRepository;
    private final UserRepository userRepository;
    private final ManagerCinemaAssignmentRepository managerCinemaAssignmentRepository;
    private final AuditLogService auditLogService;

    @Value("${app.hero.banner.max-active:7}")
    private int maxActiveBanners;

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

        // Nếu số lượng banner từ Admin ít hơn 5 (ví dụ: 2 banner, hoặc 0 banner):
        // Tự động bổ sung các phim mới nhất cho đủ mục tiêu 5 banner!
        final int targetBannerCount = 5;
        if (validResponses.size() < targetBannerCount) {
            int needed = targetBannerCount - validResponses.size();
            List<HeroBannerResponse> filledBanners = getFallbackBanners(needed, validResponses);
            List<HeroBannerResponse> combined = new ArrayList<>(validResponses);
            combined.addAll(filledBanners);
            return combined.isEmpty() ? getStaticFallback() : combined;
        }

        return validResponses;
    }

    private boolean isBannerContentEligible(HeroBanner banner) {
        if (banner.getType() == BannerType.MOVIE && banner.getLinkedMovie() != null) {
            Movie movie = banner.getLinkedMovie();
            if (movie.getStatus() == MovieStatus.INACTIVE || movie.getStatus() == MovieStatus.ENDED) {
                return false;
            }
            if (movie.getPublicationStatus() == MoviePublicationStatus.ARCHIVED) {
                return false;
            }
        }
        if (banner.getType() == BannerType.PROMOTION && banner.getLinkedPromotion() != null) {
            Promotion promotion = banner.getLinkedPromotion();
            if (promotion.getStatus() != PromotionStatus.ACTIVE ||
                    (promotion.getEndDate() != null && promotion.getEndDate().isBefore(LocalDateTime.now()))) {
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
                .filter(m -> m.getPublicationStatus() != MoviePublicationStatus.ARCHIVED)
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
                        BannerCtaType.BOOK_NOW,
                        "ĐẶT VÉ NGAY",
                        "/showtimes",
                        BannerCtaType.VIEW_MOVIE,
                        "KHÁM PHÁ PHIM",
                        "/movies",
                        "ĐẶT VÉ NGAY",
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

    // ==========================================
    // 2. ANALYTICS TRACKING
    // ==========================================

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
    // 3. ADMIN LIST & GET
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
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by("sortOrder").ascending().and(Sort.by("priority").descending()).and(Sort.by("id").ascending()));

        Specification<HeroBanner> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates = cb.and(predicates, cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("subtitle")), pattern)
                ));
            }

            if (status != null) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }

            if (type != null) {
                predicates = cb.and(predicates, cb.equal(root.get("type"), type));
            }

            if (scopeType != null) {
                predicates = cb.and(predicates, cb.equal(root.get("scopeType"), scopeType));
            }

            if (cinemaId != null && cinemaId > 0) {
                var join = root.join("bannerCinemas");
                predicates = cb.and(predicates, cb.equal(join.get("cinema").get("id"), cinemaId));
            }

            // If manager, scope to their assigned cinemas
            if (isManagerOnly(currentUser)) {
                List<Long> assignedCinemaIds = getManagerAssignedCinemaIds(currentUser.id());
                var join = root.join("bannerCinemas");
                predicates = cb.and(predicates, join.get("cinema").get("id").in(assignedCinemaIds));
            }

            return predicates;
        };

        Page<HeroBanner> pageResult = heroBannerRepository.findAll(spec, pageable);
        return PageResponse.from(pageResult.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public HeroBannerResponse getAdminBannerById(Long id, AuthenticatedUser currentUser) {
        HeroBanner banner = findBannerOrThrow(id);
        validateManagerBannerAccess(banner, currentUser);
        return toResponse(banner);
    }

    // ==========================================
    // 4. ADMIN CRUD & LIFECYCLE
    // ==========================================

    @Override
    @Transactional
    public HeroBannerResponse createBanner(CreateHeroBannerRequest request, AuthenticatedUser currentUser) {
        validateManagerCreationScope(request, currentUser);
        validateSchedule(request.startAt(), request.endAt());
        validateCta(request.primaryCtaType(), request.primaryCtaUrl());
        if (request.secondaryCtaType() != null) {
            validateCta(request.secondaryCtaType(), request.secondaryCtaUrl());
        }

        Movie linkedMovie = resolveAndValidateMovie(request.type(), request.linkedMovieId());
        Promotion linkedPromotion = resolveAndValidatePromotion(request.type(), request.linkedPromotionId());

        User actor = resolveUser(currentUser != null ? currentUser.id() : null);

        int duration = request.slideDurationSeconds() != null
                ? Math.max(MIN_DURATION, Math.min(MAX_DURATION, request.slideDurationSeconds()))
                : DEFAULT_DURATION;

        int sortOrder = request.sortOrder() != null && request.sortOrder() > 0
                ? request.sortOrder()
                : heroBannerRepository.findMaxSortOrder() + 1;

        int priority = request.priority() != null ? request.priority() : 1;

        HeroBanner banner = new HeroBanner(
                request.name().trim(),
                request.type(),
                request.title().trim(),
                request.subtitle() != null ? request.subtitle().trim() : null,
                request.desktopImageUrl().trim(),
                request.mobileImageUrl() != null ? request.mobileImageUrl().trim() : null,
                request.scopeType()
        );

        banner.setDescription(request.description());
        banner.setBadge(request.badge());
        banner.setFormatLabel(request.formatLabel());
        banner.setAltText(request.altText());
        banner.setFocalPoint(request.focalPoint() != null ? request.focalPoint() : BannerFocalPoint.CENTER);
        banner.setLinkedMovie(linkedMovie);
        banner.setLinkedPromotion(linkedPromotion);

        // CTA
        banner.setPrimaryCtaType(request.primaryCtaType());
        banner.setPrimaryCtaLabel(request.primaryCtaLabel().trim());
        banner.setPrimaryCtaUrl(resolveCtaUrl(request.primaryCtaType(), request.primaryCtaUrl(), linkedMovie, linkedPromotion));

        if (request.secondaryCtaType() != null && request.secondaryCtaType() != BannerCtaType.NONE) {
            banner.setSecondaryCtaType(request.secondaryCtaType());
            banner.setSecondaryCtaLabel(request.secondaryCtaLabel());
            banner.setSecondaryCtaUrl(resolveCtaUrl(request.secondaryCtaType(), request.secondaryCtaUrl(), linkedMovie, linkedPromotion));
        }

        banner.setTargetCity(request.targetCity());
        banner.setPriority(priority);
        banner.setSortOrder(sortOrder);
        banner.setSlideDurationSeconds(duration);
        banner.setStartAt(request.startAt());
        banner.setEndAt(request.endAt());
        banner.setCreatedBy(actor);
        banner.setUpdatedBy(actor);

        // Publish status determination
        if (Boolean.TRUE.equals(request.publishNow())) {
            validateMaxActiveBanners();
            LocalDateTime now = LocalDateTime.now();
            if (request.startAt() != null && request.startAt().isAfter(now)) {
                banner.setStatus(BannerStatus.SCHEDULED);
            } else {
                banner.setStatus(BannerStatus.PUBLISHED);
                banner.setPublishedAt(now);
            }
        } else {
            banner.setStatus(BannerStatus.DRAFT);
        }

        HeroBanner savedBanner = heroBannerRepository.save(banner);

        // Link cinemas if scope is CINEMA
        if (request.scopeType() == BannerScopeType.CINEMA && request.cinemaIds() != null && !request.cinemaIds().isEmpty()) {
            List<Cinema> cinemas = cinemaRepository.findAllById(request.cinemaIds());
            for (Cinema cinema : cinemas) {
                HeroBannerCinema hbc = new HeroBannerCinema(savedBanner, cinema);
                heroBannerCinemaRepository.save(hbc);
                savedBanner.getBannerCinemas().add(hbc);
            }
        }

        auditLogService.record(
                AuditActionType.CREATE,
                "HERO_BANNER",
                savedBanner.getId(),
                "Tạo Hero Banner: " + savedBanner.getName() + " [" + savedBanner.getType() + "]"
        );

        return toResponse(savedBanner);
    }

    @Override
    @Transactional
    public HeroBannerResponse updateBanner(Long id, UpdateHeroBannerRequest request, AuthenticatedUser currentUser) {
        HeroBanner banner = findBannerOrThrow(id);
        validateManagerBannerAccess(banner, currentUser);
        validateSchedule(request.startAt(), request.endAt());
        validateCta(request.primaryCtaType(), request.primaryCtaUrl());
        if (request.secondaryCtaType() != null) {
            validateCta(request.secondaryCtaType(), request.secondaryCtaUrl());
        }

        Movie linkedMovie = resolveAndValidateMovie(request.type(), request.linkedMovieId());
        Promotion linkedPromotion = resolveAndValidatePromotion(request.type(), request.linkedPromotionId());

        User actor = resolveUser(currentUser != null ? currentUser.id() : null);

        int duration = request.slideDurationSeconds() != null
                ? Math.max(MIN_DURATION, Math.min(MAX_DURATION, request.slideDurationSeconds()))
                : banner.getSlideDurationSeconds();

        banner.setName(request.name().trim());
        banner.setType(request.type());
        banner.setTitle(request.title().trim());
        banner.setSubtitle(request.subtitle() != null ? request.subtitle().trim() : null);
        banner.setDescription(request.description());
        banner.setBadge(request.badge());
        banner.setFormatLabel(request.formatLabel());
        banner.setDesktopImageUrl(request.desktopImageUrl().trim());
        banner.setMobileImageUrl(request.mobileImageUrl() != null ? request.mobileImageUrl().trim() : null);
        banner.setAltText(request.altText());
        banner.setFocalPoint(request.focalPoint() != null ? request.focalPoint() : BannerFocalPoint.CENTER);
        banner.setLinkedMovie(linkedMovie);
        banner.setLinkedPromotion(linkedPromotion);

        banner.setPrimaryCtaType(request.primaryCtaType());
        banner.setPrimaryCtaLabel(request.primaryCtaLabel().trim());
        banner.setPrimaryCtaUrl(resolveCtaUrl(request.primaryCtaType(), request.primaryCtaUrl(), linkedMovie, linkedPromotion));

        if (request.secondaryCtaType() != null && request.secondaryCtaType() != BannerCtaType.NONE) {
            banner.setSecondaryCtaType(request.secondaryCtaType());
            banner.setSecondaryCtaLabel(request.secondaryCtaLabel());
            banner.setSecondaryCtaUrl(resolveCtaUrl(request.secondaryCtaType(), request.secondaryCtaUrl(), linkedMovie, linkedPromotion));
        } else {
            banner.setSecondaryCtaType(BannerCtaType.NONE);
            banner.setSecondaryCtaLabel(null);
            banner.setSecondaryCtaUrl(null);
        }

        banner.setScopeType(request.scopeType());
        banner.setTargetCity(request.targetCity());
        if (request.priority() != null) banner.setPriority(request.priority());
        if (request.sortOrder() != null && request.sortOrder() > 0) banner.setSortOrder(request.sortOrder());
        banner.setSlideDurationSeconds(duration);
        banner.setStartAt(request.startAt());
        banner.setEndAt(request.endAt());
        banner.setUpdatedBy(actor);

        // Update cinemas mapping
        heroBannerCinemaRepository.deleteByHeroBannerId(banner.getId());
        banner.getBannerCinemas().clear();

        if (request.scopeType() == BannerScopeType.CINEMA && request.cinemaIds() != null && !request.cinemaIds().isEmpty()) {
            List<Cinema> cinemas = cinemaRepository.findAllById(request.cinemaIds());
            for (Cinema cinema : cinemas) {
                HeroBannerCinema hbc = new HeroBannerCinema(banner, cinema);
                heroBannerCinemaRepository.save(hbc);
                banner.getBannerCinemas().add(hbc);
            }
        }

        auditLogService.record(
                AuditActionType.UPDATE,
                "HERO_BANNER",
                banner.getId(),
                "Cập nhật Hero Banner: " + banner.getName()
        );

        return toResponse(banner);
    }

    @Override
    @Transactional
    public HeroBannerResponse publishBanner(Long id, AuthenticatedUser currentUser) {
        HeroBanner banner = findBannerOrThrow(id);
        validateManagerBannerAccess(banner, currentUser);

        if (banner.getStatus() == BannerStatus.PUBLISHED) {
            throw new BadRequestException("Banner đã được xuất bản trước đó");
        }

        LocalDateTime now = LocalDateTime.now();
        if (banner.getEndAt() != null && banner.getEndAt().isBefore(now)) {
            banner.setStatus(BannerStatus.EXPIRED);
            throw new BadRequestException("Banner đã hết hạn chiến dịch, vui lòng cập nhật lại thời gian kết thúc trước khi xuất bản");
        }

        // Validate active limit
        validateMaxActiveBanners();

        if (banner.getStartAt() != null && banner.getStartAt().isAfter(now)) {
            banner.setStatus(BannerStatus.SCHEDULED);
        } else {
            banner.setStatus(BannerStatus.PUBLISHED);
            banner.setPublishedAt(now);
        }

        banner.setUpdatedBy(resolveUser(currentUser != null ? currentUser.id() : null));

        auditLogService.record(
                AuditActionType.UPDATE,
                "HERO_BANNER",
                banner.getId(),
                "Xuất bản Hero Banner: " + banner.getName() + " -> " + banner.getStatus()
        );

        return toResponse(banner);
    }

    @Override
    @Transactional
    public HeroBannerResponse pauseBanner(Long id, AuthenticatedUser currentUser) {
        HeroBanner banner = findBannerOrThrow(id);
        validateManagerBannerAccess(banner, currentUser);

        banner.setStatus(BannerStatus.PAUSED);
        banner.setUpdatedBy(resolveUser(currentUser != null ? currentUser.id() : null));

        auditLogService.record(
                AuditActionType.UPDATE,
                "HERO_BANNER",
                banner.getId(),
                "Tạm dừng Hero Banner: " + banner.getName()
        );

        return toResponse(banner);
    }

    @Override
    @Transactional
    public HeroBannerResponse resumeBanner(Long id, AuthenticatedUser currentUser) {
        HeroBanner banner = findBannerOrThrow(id);
        validateManagerBannerAccess(banner, currentUser);

        LocalDateTime now = LocalDateTime.now();
        if (banner.getEndAt() != null && banner.getEndAt().isBefore(now)) {
            banner.setStatus(BannerStatus.EXPIRED);
            throw new BadRequestException("Không thể tiếp tục banner vì thời gian chiến dịch đã hết hạn. Vui lòng cập nhật thời gian kết thúc.");
        }

        validateMaxActiveBanners();

        if (banner.getStartAt() != null && banner.getStartAt().isAfter(now)) {
            banner.setStatus(BannerStatus.SCHEDULED);
        } else {
            banner.setStatus(BannerStatus.PUBLISHED);
        }

        banner.setUpdatedBy(resolveUser(currentUser != null ? currentUser.id() : null));

        auditLogService.record(
                AuditActionType.UPDATE,
                "HERO_BANNER",
                banner.getId(),
                "Tiếp tục chạy Hero Banner: " + banner.getName()
        );

        return toResponse(banner);
    }

    @Override
    @Transactional
    public HeroBannerResponse archiveBanner(Long id, AuthenticatedUser currentUser) {
        HeroBanner banner = findBannerOrThrow(id);
        validateManagerBannerAccess(banner, currentUser);

        banner.setStatus(BannerStatus.ARCHIVED);
        banner.setUpdatedBy(resolveUser(currentUser != null ? currentUser.id() : null));

        auditLogService.record(
                AuditActionType.UPDATE,
                "HERO_BANNER",
                banner.getId(),
                "Lưu trữ Hero Banner: " + banner.getName()
        );

        return toResponse(banner);
    }

    @Override
    @Transactional
    public HeroBannerResponse duplicateBanner(Long id, AuthenticatedUser currentUser) {
        HeroBanner source = findBannerOrThrow(id);
        validateManagerBannerAccess(source, currentUser);

        User actor = resolveUser(currentUser != null ? currentUser.id() : null);

        HeroBanner copy = new HeroBanner(
                source.getName() + " (Bản sao)",
                source.getType(),
                source.getTitle(),
                source.getSubtitle(),
                source.getDesktopImageUrl(),
                source.getMobileImageUrl(),
                source.getScopeType()
        );

        copy.setDescription(source.getDescription());
        copy.setBadge(source.getBadge());
        copy.setFormatLabel(source.getFormatLabel());
        copy.setAltText(source.getAltText());
        copy.setFocalPoint(source.getFocalPoint());
        copy.setLinkedMovie(source.getLinkedMovie());
        copy.setLinkedPromotion(source.getLinkedPromotion());
        copy.setPrimaryCtaType(source.getPrimaryCtaType());
        copy.setPrimaryCtaLabel(source.getPrimaryCtaLabel());
        copy.setPrimaryCtaUrl(source.getPrimaryCtaUrl());
        copy.setSecondaryCtaType(source.getSecondaryCtaType());
        copy.setSecondaryCtaLabel(source.getSecondaryCtaLabel());
        copy.setSecondaryCtaUrl(source.getSecondaryCtaUrl());
        copy.setTargetCity(source.getTargetCity());
        copy.setPriority(source.getPriority());
        copy.setSortOrder(heroBannerRepository.findMaxSortOrder() + 1);
        copy.setSlideDurationSeconds(source.getSlideDurationSeconds());
        copy.setStatus(BannerStatus.DRAFT);
        copy.setImpressionsCount(0);
        copy.setClicksCount(0);
        copy.setCreatedBy(actor);
        copy.setUpdatedBy(actor);

        HeroBanner savedCopy = heroBannerRepository.save(copy);

        // Copy cinema associations
        for (HeroBannerCinema hbc : source.getBannerCinemas()) {
            HeroBannerCinema copyHbc = new HeroBannerCinema(savedCopy, hbc.getCinema());
            heroBannerCinemaRepository.save(copyHbc);
            savedCopy.getBannerCinemas().add(copyHbc);
        }

        auditLogService.record(
                AuditActionType.CREATE,
                "HERO_BANNER",
                savedCopy.getId(),
                "Nhân bản Hero Banner từ ID #" + source.getId() + " thành " + savedCopy.getName()
        );

        return toResponse(savedCopy);
    }

    @Override
    @Transactional
    public void reorderBanners(List<Long> bannerIds, AuthenticatedUser currentUser) {
        if (bannerIds == null || bannerIds.isEmpty()) return;

        for (int i = 0; i < bannerIds.size(); i++) {
            Long bannerId = bannerIds.get(i);
            int newOrder = i + 1;
            heroBannerRepository.findById(bannerId).ifPresent(b -> b.setSortOrder(newOrder));
        }

        auditLogService.record(
                AuditActionType.UPDATE,
                "HERO_BANNER",
                bannerIds.get(0),
                "Cập nhật thứ tự hiển thị Hero Banner (" + bannerIds.size() + " banners)"
        );
    }

    @Override
    @Transactional
    public void deleteDraftBanner(Long id, AuthenticatedUser currentUser) {
        HeroBanner banner = findBannerOrThrow(id);
        validateManagerBannerAccess(banner, currentUser);

        if (banner.getStatus() != BannerStatus.DRAFT) {
            throw new BadRequestException("Chỉ được xóa các banner ở trạng thái Bản nháp (DRAFT). Banner đã xuất bản vui lòng đưa vào Lưu trữ.");
        }

        if (banner.getImpressionsCount() > 0 || banner.getClicksCount() > 0) {
            throw new BadRequestException("Banner đã từng có số liệu thống kê thực tế, vui lòng đưa vào Lưu trữ thay vì xóa vĩnh viễn.");
        }

        heroBannerCinemaRepository.deleteByHeroBannerId(banner.getId());
        heroBannerRepository.delete(banner);

        auditLogService.record(
                AuditActionType.DELETE,
                "HERO_BANNER",
                id,
                "Xóa bản nháp Hero Banner: " + banner.getName()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getBannerSummaryStats() {
        LocalDateTime now = LocalDateTime.now();
        List<HeroBanner> all = heroBannerRepository.findAll();

        long activeCount = all.stream().filter(b -> b.getStatus() == BannerStatus.PUBLISHED).count();
        long scheduledCount = all.stream().filter(b -> b.getStatus() == BannerStatus.SCHEDULED).count();
        long draftCount = all.stream().filter(b -> b.getStatus() == BannerStatus.DRAFT).count();
        long pausedCount = all.stream().filter(b -> b.getStatus() == BannerStatus.PAUSED).count();
        long expiredCount = all.stream().filter(b -> b.getStatus() == BannerStatus.EXPIRED || (b.getEndAt() != null && b.getEndAt().isBefore(now))).count();
        long archivedCount = all.stream().filter(b -> b.getStatus() == BannerStatus.ARCHIVED).count();

        long totalImpressions = all.stream().mapToLong(HeroBanner::getImpressionsCount).sum();
        long totalClicks = all.stream().mapToLong(HeroBanner::getClicksCount).sum();
        double avgCtr = totalImpressions > 0 ? ((double) totalClicks / totalImpressions) * 100.0 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("active", activeCount);
        stats.put("scheduled", scheduledCount);
        stats.put("draft", draftCount);
        stats.put("paused", pausedCount);
        stats.put("expired", expiredCount);
        stats.put("archived", archivedCount);
        stats.put("totalBanners", all.size());
        stats.put("totalImpressions", totalImpressions);
        stats.put("totalClicks", totalClicks);
        stats.put("avgCtr", Math.round(avgCtr * 100.0) / 100.0);
        stats.put("maxActiveLimit", maxActiveBanners);

        return stats;
    }

    // ==========================================
    // 5. HELPER VALIDATIONS & MAPPERS
    // ==========================================

    private void validateMaxActiveBanners() {
        LocalDateTime now = LocalDateTime.now();
        long currentlyActive = heroBannerRepository.countCurrentlyActive(BannerStatus.PUBLISHED, now);
        if (currentlyActive >= maxActiveBanners) {
            throw new BadRequestException("MAX_ACTIVE_BANNERS_REACHED: Đã đạt giới hạn tối đa " + maxActiveBanners +
                    " banner đang hoạt động cùng thời điểm. Vui lòng tạm dừng hoặc gỡ bớt banner trước khi xuất bản thêm.");
        }
    }

    private void validateSchedule(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new BadRequestException("INVALID_BANNER_SCHEDULE: Thời gian kết thúc phải lớn hơn thời gian bắt đầu.");
        }
    }

    private void validateCta(BannerCtaType type, String url) {
        if (type == BannerCtaType.EXTERNAL_URL) {
            if (url == null || url.isBlank()) {
                throw new BadRequestException("URL ngoài không được để trống khi chọn loại EXTERNAL_URL.");
            }
            try {
                URI uri = URI.create(url.trim());
                String scheme = uri.getScheme();
                if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                    throw new BadRequestException("URL ngoài chỉ chấp nhận giao thức http:// hoặc https://. Không cho phép javascript: hoặc giao thức không an toàn.");
                }
            } catch (BadRequestException e) {
                throw e;
            } catch (Exception e) {
                throw new BadRequestException("Định dạng URL không hợp lệ: " + e.getMessage());
            }
        }
    }

    private Movie resolveAndValidateMovie(BannerType type, Long movieId) {
        if (type == BannerType.MOVIE) {
            if (movieId == null) {
                throw new BadRequestException("MOVIE_NOT_ELIGIBLE_FOR_BANNER: Phải chọn một bộ phim liên kết khi loại banner là MOVIE.");
            }
            Movie movie = movieRepository.findById(movieId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy phim với ID: " + movieId));

            if (movie.getStatus() == MovieStatus.INACTIVE) {
                throw new BadRequestException("MOVIE_NOT_ELIGIBLE_FOR_BANNER: Phim đang ở trạng thái INACTIVE không đủ điều kiện lên Hero Banner.");
            }
            if (movie.getPublicationStatus() == MoviePublicationStatus.ARCHIVED) {
                throw new BadRequestException("MOVIE_NOT_ELIGIBLE_FOR_BANNER: Phim đã bị lưu trữ (ARCHIVED) không thể đưa lên Hero Banner.");
            }
            return movie;
        }
        if (movieId != null) {
            return movieRepository.findById(movieId).orElse(null);
        }
        return null;
    }

    private Promotion resolveAndValidatePromotion(BannerType type, Long promoId) {
        if (type == BannerType.PROMOTION) {
            if (promoId == null) {
                return null;
            }
            Promotion promo = promotionRepository.findById(promoId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy khuyến mãi với ID: " + promoId));

            if (promo.getStatus() != PromotionStatus.ACTIVE ||
                    (promo.getEndDate() != null && promo.getEndDate().isBefore(LocalDateTime.now()))) {
                throw new BadRequestException("PROMOTION_NOT_ELIGIBLE_FOR_BANNER: Khuyến mãi đã hết hạn hoặc không hoạt động.");
            }
            return promo;
        }
        if (promoId != null) {
            return promotionRepository.findById(promoId).orElse(null);
        }
        return null;
    }

    private String resolveCtaUrl(BannerCtaType ctaType, String customUrl, Movie movie, Promotion promotion) {
        if (customUrl != null && !customUrl.isBlank()) {
            return customUrl.trim();
        }
        if (ctaType == null) return null;

        return switch (ctaType) {
            case BOOK_NOW -> movie != null
                    ? (movie.getStatus() == MovieStatus.NOW_SHOWING ? "/showtimes?movieId=" + movie.getId() : "/movies/" + movie.getId())
                    : "/showtimes";
            case VIEW_MOVIE -> movie != null ? "/movies/" + movie.getId() : "/movies";
            case VIEW_PROMOTION -> promotion != null ? "/policies" : "/policies";
            case VIEW_FNB -> "/concessions";
            case VIEW_EVENT, MEMBERSHIP -> "/policies";
            case INTERNAL_URL, EXTERNAL_URL -> customUrl;
            case NONE -> null;
        };
    }

    private HeroBanner findBannerOrThrow(Long id) {
        return heroBannerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("HERO_BANNER_NOT_FOUND: Không tìm thấy Hero Banner với ID #" + id));
    }

    private User resolveUser(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }

    private boolean isManagerOnly(AuthenticatedUser user) {
        if (user == null || user.getAuthorities() == null) return false;
        boolean isManager = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
        boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return isManager && !isAdmin;
    }

    private List<Long> getManagerAssignedCinemaIds(Long userId) {
        if (userId == null) return List.of();
        return managerCinemaAssignmentRepository.findByUserId(userId).stream()
                .map(a -> a.getCinema().getId())
                .toList();
    }

    private void validateManagerCreationScope(CreateHeroBannerRequest request, AuthenticatedUser currentUser) {
        if (!isManagerOnly(currentUser)) return;

        if (request.scopeType() == BannerScopeType.GLOBAL) {
            throw new ForbiddenException("CINEMA_ACCESS_DENIED: Quản lý cụm rạp không được phép tạo Banner hiển thị toàn hệ thống (GLOBAL). Chỉ có Quản trị viên (ADMIN) mới có quyền này.");
        }

        List<Long> assignedCinemaIds = getManagerAssignedCinemaIds(currentUser.id());
        if (request.scopeType() == BannerScopeType.CINEMA) {
            if (request.cinemaIds() == null || request.cinemaIds().isEmpty()) {
                throw new BadRequestException("Vui lòng chọn ít nhất một cụm rạp được phân công quản lý.");
            }
            for (Long cinemaId : request.cinemaIds()) {
                if (!assignedCinemaIds.contains(cinemaId)) {
                    throw new ForbiddenException("CINEMA_ACCESS_DENIED: Bạn không có quyền quản lý banner cho cụm rạp ID #" + cinemaId);
                }
            }
        }
    }

    private void validateManagerBannerAccess(HeroBanner banner, AuthenticatedUser currentUser) {
        if (!isManagerOnly(currentUser)) return;

        List<Long> assignedCinemaIds = getManagerAssignedCinemaIds(currentUser.id());
        boolean hasAccess = banner.getBannerCinemas().stream()
                .anyMatch(hbc -> assignedCinemaIds.contains(hbc.getCinema().getId()));

        if (!hasAccess) {
            throw new ForbiddenException("CINEMA_ACCESS_DENIED: Bạn không có quyền truy cập hoặc chỉnh sửa banner này.");
        }
    }

    private HeroBannerResponse toResponse(HeroBanner b) {
        double ctr = b.getImpressionsCount() > 0
                ? ((double) b.getClicksCount() / b.getImpressionsCount()) * 100.0
                : 0.0;
        ctr = Math.round(ctr * 100.0) / 100.0;

        Movie movie = b.getLinkedMovie();
        Promotion promo = b.getLinkedPromotion();

        List<BannerCinemaSummary> cinemas = b.getBannerCinemas() != null
                ? b.getBannerCinemas().stream()
                .map(bc -> new BannerCinemaSummary(bc.getCinema().getId(), bc.getCinema().getName(), bc.getCinema().getCity()))
                .toList()
                : List.of();

        String desktopImg = b.getDesktopImageUrl();
        String mobileImg = b.getMobileImageUrl() != null && !b.getMobileImageUrl().isBlank()
                ? b.getMobileImageUrl()
                : desktopImg;

        return new HeroBannerResponse(
                b.getId(),
                b.getName(),
                b.getType(),
                b.getTitle(),
                b.getSubtitle(),
                b.getDescription(),
                b.getBadge(),
                b.getFormatLabel(),
                desktopImg,
                b.getMobileImageUrl(),
                b.getAltText(),
                b.getFocalPoint(),
                desktopImg,
                mobileImg,
                b.getFormatLabel(),
                movie != null ? movie.getId() : null,
                movie != null ? movie.getId() : null,
                movie != null ? movie.getTitle() : null,
                movie != null ? movie.getPosterUrl() : null,
                movie != null ? movie.getTrailerUrl() : null,
                movie != null ? movie.getDurationMinutes() : null,
                movie != null && movie.getAgeRating() != null ? movie.getAgeRating().name() : null,
                movie != null ? movie.getStatus().name() : null,
                promo != null ? promo.getId() : null,
                promo != null ? promo.getCode() : null,
                promo != null ? promo.getName() : null,
                b.getPrimaryCtaType(),
                b.getPrimaryCtaLabel(),
                b.getPrimaryCtaUrl(),
                b.getSecondaryCtaType(),
                b.getSecondaryCtaLabel(),
                b.getSecondaryCtaUrl(),
                b.getPrimaryCtaLabel(),
                b.getPrimaryCtaUrl(),
                b.getScopeType(),
                b.getTargetCity(),
                cinemas,
                b.getPriority(),
                b.getSortOrder(),
                b.getSlideDurationSeconds(),
                b.getStartAt(),
                b.getEndAt(),
                b.getStatus(),
                b.getImpressionsCount(),
                b.getClicksCount(),
                ctr,
                b.getCreatedBy() != null ? b.getCreatedBy().getId() : null,
                b.getCreatedBy() != null ? b.getCreatedBy().getFullName() : null,
                b.getUpdatedBy() != null ? b.getUpdatedBy().getId() : null,
                b.getUpdatedBy() != null ? b.getUpdatedBy().getFullName() : null,
                b.getPublishedAt(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
