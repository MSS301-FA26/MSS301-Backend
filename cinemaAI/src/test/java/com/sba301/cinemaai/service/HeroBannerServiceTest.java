package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.banner.CreateHeroBannerRequest;
import com.sba301.cinemaai.dto.request.banner.UpdateHeroBannerRequest;
import com.sba301.cinemaai.dto.response.banner.HeroBannerResponse;
import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.entity.HeroBanner;
import com.sba301.cinemaai.entity.HeroBannerCinema;
import com.sba301.cinemaai.entity.ManagerCinemaAssignment;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.BannerCtaType;
import com.sba301.cinemaai.enums.BannerFocalPoint;
import com.sba301.cinemaai.enums.BannerScopeType;
import com.sba301.cinemaai.enums.BannerStatus;
import com.sba301.cinemaai.enums.BannerType;
import com.sba301.cinemaai.enums.MoviePublicationStatus;
import com.sba301.cinemaai.enums.MovieStatus;
import com.sba301.cinemaai.enums.UserStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.ForbiddenException;
import com.sba301.cinemaai.repository.CinemaRepository;
import com.sba301.cinemaai.repository.HeroBannerCinemaRepository;
import com.sba301.cinemaai.repository.HeroBannerRepository;
import com.sba301.cinemaai.repository.ManagerCinemaAssignmentRepository;
import com.sba301.cinemaai.repository.MovieRepository;
import com.sba301.cinemaai.repository.PromotionRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.impl.HeroBannerServiceImpl;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeroBannerServiceTest {

    @Mock
    private HeroBannerRepository heroBannerRepository;
    @Mock
    private HeroBannerCinemaRepository heroBannerCinemaRepository;
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private CinemaRepository cinemaRepository;
    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ManagerCinemaAssignmentRepository managerCinemaAssignmentRepository;
    @Mock
    private AuditLogService auditLogService;

    private HeroBannerServiceImpl heroBannerService;

    private AuthenticatedUser adminUser;
    private AuthenticatedUser managerUser;

    @BeforeEach
    void setUp() throws Exception {
        heroBannerService = new HeroBannerServiceImpl(
                heroBannerRepository,
                heroBannerCinemaRepository,
                movieRepository,
                cinemaRepository,
                promotionRepository,
                userRepository,
                managerCinemaAssignmentRepository,
                auditLogService
        );

        // Set maxActiveBanners via reflection to 7
        Field field = HeroBannerServiceImpl.class.getDeclaredField("maxActiveBanners");
        field.setAccessible(true);
        field.set(heroBannerService, 7);

        adminUser = new AuthenticatedUser(
                1L, "admin@cinepremier.com", "pass", UserStatus.ACTIVE, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        managerUser = new AuthenticatedUser(
                2L, "manager@cinepremier.com", "pass", UserStatus.ACTIVE, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
        );
    }

    @Test
    @DisplayName("Admin creates a Movie banner successfully")
    void testAdminCreateMovieBanner() {
        Movie movie = new Movie("Dune Part Two", 166, MovieStatus.NOW_SHOWING);
        setEntityId(movie, 10L);
        movie.setPublicationStatus(MoviePublicationStatus.PUBLISHED);

        when(movieRepository.findById(10L)).thenReturn(Optional.of(movie));
        when(heroBannerRepository.save(any(HeroBanner.class))).thenAnswer(inv -> {
            HeroBanner b = inv.getArgument(0);
            setEntityId(b, 100L);
            return b;
        });

        CreateHeroBannerRequest request = new CreateHeroBannerRequest(
                "Dune Banner",
                BannerType.MOVIE,
                "Dune Part Two",
                "Trải nghiệm IMAX đỉnh cao",
                "Bom tấn viễn tưởng",
                "BOM TẤN ĐANG CHIẾU",
                "IMAX 70MM",
                "https://image.tmdb.org/dune.jpg",
                "https://image.tmdb.org/dune-m.jpg",
                "Dune banner",
                BannerFocalPoint.CENTER,
                10L,
                null,
                BannerCtaType.BOOK_NOW,
                "ĐẶT VÉ NGAY",
                null,
                BannerCtaType.VIEW_MOVIE,
                "XEM CHI TIẾT",
                null,
                BannerScopeType.GLOBAL,
                null,
                null,
                5,
                1,
                6,
                null,
                null,
                false
        );

        HeroBannerResponse response = heroBannerService.createBanner(request, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(BannerStatus.DRAFT);
        assertThat(response.type()).isEqualTo(BannerType.MOVIE);
        assertThat(response.linkedMovieId()).isEqualTo(10L);
        assertThat(response.primaryCtaUrl()).isEqualTo("/showtimes?movieId=10");
        assertThat(response.secondaryCtaUrl()).isEqualTo("/movies/10");
    }

    @Test
    @DisplayName("Admin creates Custom banner with validated external URL")
    void testAdminCreateCustomBannerWithExternalUrl() {
        when(heroBannerRepository.save(any(HeroBanner.class))).thenAnswer(inv -> {
            HeroBanner b = inv.getArgument(0);
            setEntityId(b, 101L);
            return b;
        });

        CreateHeroBannerRequest request = new CreateHeroBannerRequest(
                "VPBank Promo Banner",
                BannerType.CUSTOM,
                "Ưu đãi VPBank",
                "Giảm 100K khi thanh toán thẻ VPBank",
                null,
                "ƯU ĐÃI",
                null,
                "https://cdn.example.com/vpbank.jpg",
                null,
                "VPBank",
                BannerFocalPoint.CENTER,
                null,
                null,
                BannerCtaType.EXTERNAL_URL,
                "XEM NGAY",
                "https://vpbank.com.vn/cinepremier",
                null,
                null,
                null,
                BannerScopeType.GLOBAL,
                null,
                null,
                1,
                1,
                6,
                null,
                null,
                false
        );

        HeroBannerResponse response = heroBannerService.createBanner(request, adminUser);
        assertThat(response.primaryCtaUrl()).isEqualTo("https://vpbank.com.vn/cinepremier");
    }

    @Test
    @DisplayName("Reject invalid external URL scheme like javascript:")
    void testRejectInvalidExternalUrlScheme() {
        CreateHeroBannerRequest request = new CreateHeroBannerRequest(
                "Bad Banner",
                BannerType.CUSTOM,
                "Bad Title",
                null,
                null,
                null,
                null,
                "https://cdn.example.com/bad.jpg",
                null,
                null,
                BannerFocalPoint.CENTER,
                null,
                null,
                BannerCtaType.EXTERNAL_URL,
                "CLICK",
                "javascript:alert(1)",
                null,
                null,
                null,
                BannerScopeType.GLOBAL,
                null,
                null,
                1,
                1,
                6,
                null,
                null,
                false
        );

        assertThatThrownBy(() -> heroBannerService.createBanner(request, adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("giao thức http:// hoặc https://");
    }

    @Test
    @DisplayName("Reject Movie banner if movie is INACTIVE or ARCHIVED")
    void testRejectInactiveOrArchivedMovie() {
        Movie inactiveMovie = new Movie("Old Movie", 100, MovieStatus.INACTIVE);
        setEntityId(inactiveMovie, 5L);

        when(movieRepository.findById(5L)).thenReturn(Optional.of(inactiveMovie));

        CreateHeroBannerRequest request = new CreateHeroBannerRequest(
                "Inactive Movie Banner",
                BannerType.MOVIE,
                "Old Movie",
                null,
                null,
                null,
                null,
                "https://cdn.example.com/img.jpg",
                null,
                null,
                BannerFocalPoint.CENTER,
                5L,
                null,
                BannerCtaType.BOOK_NOW,
                "ĐẶT VÉ",
                null,
                null,
                null,
                null,
                BannerScopeType.GLOBAL,
                null,
                null,
                1,
                1,
                6,
                null,
                null,
                false
        );

        assertThatThrownBy(() -> heroBannerService.createBanner(request, adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("MOVIE_NOT_ELIGIBLE_FOR_BANNER");
    }

    @Test
    @DisplayName("Manager forbidden from creating GLOBAL banner")
    void testManagerForbiddenFromGlobalBanner() {
        CreateHeroBannerRequest request = new CreateHeroBannerRequest(
                "Manager Global Banner",
                BannerType.CUSTOM,
                "Title",
                null,
                null,
                null,
                null,
                "https://cdn.example.com/img.jpg",
                null,
                null,
                BannerFocalPoint.CENTER,
                null,
                null,
                BannerCtaType.BOOK_NOW,
                "ĐẶT VÉ",
                null,
                null,
                null,
                null,
                BannerScopeType.GLOBAL, // Manager tries GLOBAL
                null,
                null,
                1,
                1,
                6,
                null,
                null,
                false
        );

        assertThatThrownBy(() -> heroBannerService.createBanner(request, managerUser))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("CINEMA_ACCESS_DENIED");
    }

    @Test
    @DisplayName("Manager forbidden from creating banner for unassigned cinema")
    void testManagerForbiddenFromUnassignedCinema() {
        User user = new User("manager@cinepremier.com", "pass", "Manager", "0900000000");
        Cinema cinema1 = new Cinema("CinePremier Q1", "Q1", "TP.HCM", "028123456");
        setEntityId(cinema1, 1L);

        when(managerCinemaAssignmentRepository.findByUserId(2L)).thenReturn(List.of(
                new ManagerCinemaAssignment(user, cinema1)
        ));

        CreateHeroBannerRequest request = new CreateHeroBannerRequest(
                "Manager Q7 Banner",
                BannerType.CUSTOM,
                "Title",
                null,
                null,
                null,
                null,
                "https://cdn.example.com/img.jpg",
                null,
                null,
                BannerFocalPoint.CENTER,
                null,
                null,
                BannerCtaType.BOOK_NOW,
                "ĐẶT VÉ",
                null,
                null,
                null,
                null,
                BannerScopeType.CINEMA,
                null,
                List.of(7L), // Unassigned cinema ID
                1,
                1,
                6,
                null,
                null,
                false
        );

        assertThatThrownBy(() -> heroBannerService.createBanner(request, managerUser))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("CINEMA_ACCESS_DENIED");
    }

    @Test
    @DisplayName("Publishing banner when max active reached throws BadRequestException")
    void testMaxActiveBannersEnforced() {
        HeroBanner banner = new HeroBanner("Draft", BannerType.CUSTOM, "Title", "Sub", "img", null, BannerScopeType.GLOBAL);
        setEntityId(banner, 1L);
        banner.setStatus(BannerStatus.DRAFT);

        when(heroBannerRepository.findById(1L)).thenReturn(Optional.of(banner));
        when(heroBannerRepository.countCurrentlyActive(eq(BannerStatus.PUBLISHED), any(LocalDateTime.class))).thenReturn(7L);

        assertThatThrownBy(() -> heroBannerService.publishBanner(1L, adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("MAX_ACTIVE_BANNERS_REACHED");
    }

    @Test
    @DisplayName("Pause and Resume banner lifecycle")
    void testPauseAndResumeBanner() {
        HeroBanner banner = new HeroBanner("Active", BannerType.CUSTOM, "Title", "Sub", "img", null, BannerScopeType.GLOBAL);
        setEntityId(banner, 2L);
        banner.setStatus(BannerStatus.PUBLISHED);

        when(heroBannerRepository.findById(2L)).thenReturn(Optional.of(banner));

        // Pause
        HeroBannerResponse paused = heroBannerService.pauseBanner(2L, adminUser);
        assertThat(paused.status()).isEqualTo(BannerStatus.PAUSED);

        // Resume
        when(heroBannerRepository.countCurrentlyActive(eq(BannerStatus.PUBLISHED), any(LocalDateTime.class))).thenReturn(3L);
        HeroBannerResponse resumed = heroBannerService.resumeBanner(2L, adminUser);
        assertThat(resumed.status()).isEqualTo(BannerStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Duplicate banner resets status to DRAFT and analytics to 0")
    void testDuplicateBanner() {
        HeroBanner original = new HeroBanner("Original", BannerType.CUSTOM, "Orig Title", "Orig Sub", "img.jpg", "mob.jpg", BannerScopeType.GLOBAL);
        setEntityId(original, 50L);
        original.setStatus(BannerStatus.PUBLISHED);
        original.setImpressionsCount(1500);
        original.setClicksCount(120);

        when(heroBannerRepository.findById(50L)).thenReturn(Optional.of(original));
        when(heroBannerRepository.findMaxSortOrder()).thenReturn(4);
        when(heroBannerRepository.save(any(HeroBanner.class))).thenAnswer(inv -> {
            HeroBanner b = inv.getArgument(0);
            setEntityId(b, 51L);
            return b;
        });

        HeroBannerResponse copy = heroBannerService.duplicateBanner(50L, adminUser);

        assertThat(copy.id()).isEqualTo(51L);
        assertThat(copy.name()).isEqualTo("Original (Bản sao)");
        assertThat(copy.status()).isEqualTo(BannerStatus.DRAFT);
        assertThat(copy.impressionsCount()).isEqualTo(0);
        assertThat(copy.clicksCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Reorder banners updates sortOrder correctly")
    void testReorderBanners() {
        HeroBanner b1 = new HeroBanner("B1", BannerType.CUSTOM, "T1", "S1", "img1", null, BannerScopeType.GLOBAL);
        setEntityId(b1, 1L);
        HeroBanner b2 = new HeroBanner("B2", BannerType.CUSTOM, "T2", "S2", "img2", null, BannerScopeType.GLOBAL);
        setEntityId(b2, 2L);

        when(heroBannerRepository.findById(1L)).thenReturn(Optional.of(b1));
        when(heroBannerRepository.findById(2L)).thenReturn(Optional.of(b2));

        heroBannerService.reorderBanners(List.of(2L, 1L), adminUser);

        assertThat(b2.getSortOrder()).isEqualTo(1);
        assertThat(b1.getSortOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("Cannot delete published banner with history, must archive")
    void testCannotDeletePublishedBannerWithStats() {
        HeroBanner banner = new HeroBanner("Live Banner", BannerType.CUSTOM, "Title", "Sub", "img", null, BannerScopeType.GLOBAL);
        setEntityId(banner, 99L);
        banner.setStatus(BannerStatus.PUBLISHED);

        when(heroBannerRepository.findById(99L)).thenReturn(Optional.of(banner));

        assertThatThrownBy(() -> heroBannerService.deleteDraftBanner(99L, adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Chỉ được xóa các banner ở trạng thái Bản nháp");
    }

    private void setEntityId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
