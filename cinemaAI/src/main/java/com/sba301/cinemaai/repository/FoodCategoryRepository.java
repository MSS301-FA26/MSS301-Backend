package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.FoodCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {

    List<FoodCategory> findByDeletedAtIsNullOrderBySortOrderAsc();

    Optional<FoodCategory> findByIdAndDeletedAtIsNull(Long id);

    Optional<FoodCategory> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
