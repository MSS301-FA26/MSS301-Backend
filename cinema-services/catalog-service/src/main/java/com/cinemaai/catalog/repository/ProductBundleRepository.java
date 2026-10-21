package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.ProductBundle;
import com.cinemaai.catalog.enums.ProductStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductBundleRepository extends JpaRepository<ProductBundle, Long> {

    Optional<ProductBundle> findByNameIgnoreCase(String name);

    List<ProductBundle> findByStatus(ProductStatus status);

    Page<ProductBundle> findByStatus(ProductStatus status, Pageable pageable);

    List<ProductBundle> findByStatusIn(Collection<ProductStatus> statuses);

    Page<ProductBundle> findByStatusIn(Collection<ProductStatus> statuses, Pageable pageable);
}
