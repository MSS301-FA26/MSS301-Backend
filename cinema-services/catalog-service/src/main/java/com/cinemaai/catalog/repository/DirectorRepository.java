package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.Director;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectorRepository extends JpaRepository<Director, Long> {

    Optional<Director> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Page<Director> findByNameContainingIgnoreCaseOrderByNameAsc(String keyword, Pageable pageable);
}
