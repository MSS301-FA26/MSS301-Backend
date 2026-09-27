package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.FoodItem;
import com.sba301.cinemaai.enums.FoodItemStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    Optional<FoodItem> findByNameIgnoreCase(String name);

    Optional<FoodItem> findBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    long countByCategoryIdAndDeletedAtIsNull(Long categoryId);

    List<FoodItem> findByStatus(FoodItemStatus status);

    Page<FoodItem> findByStatus(FoodItemStatus status, Pageable pageable);

    List<FoodItem> findByStatusIn(Collection<FoodItemStatus> statuses);

    Page<FoodItem> findByStatusIn(Collection<FoodItemStatus> statuses, Pageable pageable);

    List<FoodItem> findByDeletedAtIsNull();

    Page<FoodItem> findByDeletedAtIsNull(Pageable pageable);

    Optional<FoodItem> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
           SELECT f FROM FoodItem f
           WHERE (:includeDeleted = true OR f.deletedAt IS NULL)
             AND (:search IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(f.sku) LIKE LOWER(CONCAT('%', :search, '%')))
             AND (:categoryId IS NULL OR f.category.id = :categoryId)
             AND (:status IS NULL OR f.status = :status)
           """)
    Page<FoodItem> searchItems(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("status") FoodItemStatus status,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable
    );
}
