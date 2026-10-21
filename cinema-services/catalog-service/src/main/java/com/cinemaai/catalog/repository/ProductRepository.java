package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.Product;
import com.cinemaai.catalog.enums.ProductStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByNameIgnoreCase(String name);

    List<Product> findByStatus(ProductStatus status);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    List<Product> findByStatusIn(Collection<ProductStatus> statuses);

    Page<Product> findByStatusIn(Collection<ProductStatus> statuses, Pageable pageable);
}
