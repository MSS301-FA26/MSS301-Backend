package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);

    boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

    boolean existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(String code, Long id);

    List<Promotion> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Promotion> findAllByDeletedAtIsNotNullOrderByDeletedAtDesc();

    @Query("SELECT p FROM Promotion p WHERE p.deletedAt IS NULL AND p.status = 'ACTIVE' AND p.startDate <= :now AND p.endDate >= :now ORDER BY p.createdAt DESC")
    List<Promotion> findActivePromotions(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.deletedAt IS NULL")
    long countTotalPromotions();

    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.deletedAt IS NULL AND p.status = 'ACTIVE' AND p.startDate <= :now AND p.endDate >= :now")
    long countActivePromotions(@Param("now") LocalDateTime now);
}
