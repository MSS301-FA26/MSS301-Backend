package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    List<ProductCategory> findByStatusOrderBySortOrderAscNameAsc(String status);
    List<ProductCategory> findAllByOrderBySortOrderAscNameAsc();
    Optional<ProductCategory> findByCode(String code);
    boolean existsByCode(String code);
}
