package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.FoodCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {
    List<FoodCategory> findByStatusOrderBySortOrderAscNameAsc(String status);
    List<FoodCategory> findAllByOrderBySortOrderAscNameAsc();
    Optional<FoodCategory> findByCode(String code);
    boolean existsByCode(String code);
}
