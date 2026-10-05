package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.HeroBanner;
import com.cinemaai.catalog.enums.BannerStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HeroBannerRepository extends JpaRepository<HeroBanner, Long>, JpaSpecificationExecutor<HeroBanner> {

    List<HeroBanner> findByStatusOrderBySortOrderAscPriorityDescIdAsc(BannerStatus status);

    @Query("SELECT b FROM HeroBanner b WHERE b.status = :status AND " +
            "(b.startAt IS NULL OR b.startAt <= :now) AND " +
            "(b.endAt IS NULL OR b.endAt >= :now) " +
            "ORDER BY b.sortOrder ASC, b.priority DESC, b.id ASC")
    List<HeroBanner> findActivePublicBanners(
            @Param("status") BannerStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT DISTINCT b FROM HeroBanner b " +
            "LEFT JOIN b.bannerCinemas bc " +
            "WHERE b.status = :status " +
            "AND (b.startAt IS NULL OR b.startAt <= :now) " +
            "AND (b.endAt IS NULL OR b.endAt >= :now) " +
            "AND (" +
            "   b.scopeType = 'GLOBAL' " +
            "   OR (b.scopeType = 'CITY' AND LOWER(b.targetCity) = LOWER(:city)) " +
            "   OR (b.scopeType = 'CINEMA' AND bc.cinema.id = :cinemaId)" +
            ") " +
            "ORDER BY b.sortOrder ASC, b.priority DESC, b.id ASC")
    List<HeroBanner> findActivePublicBannersForCinema(
            @Param("status") BannerStatus status,
            @Param("now") LocalDateTime now,
            @Param("cinemaId") Long cinemaId,
            @Param("city") String city
    );

    @Query("SELECT COUNT(b) FROM HeroBanner b WHERE b.status = :status AND " +
            "(b.startAt IS NULL OR b.startAt <= :now) AND " +
            "(b.endAt IS NULL OR b.endAt >= :now)")
    long countCurrentlyActive(
            @Param("status") BannerStatus status,
            @Param("now") LocalDateTime now
    );

    long countByStatus(BannerStatus status);

    @Query("SELECT COALESCE(MAX(b.sortOrder), 0) FROM HeroBanner b")
    int findMaxSortOrder();

    List<HeroBanner> findByLinkedMovieId(Long movieId);
}
