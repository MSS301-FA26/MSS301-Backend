package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.FoodCombo;
import com.cinemaai.catalog.enums.FoodItemStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodComboRepository extends JpaRepository<FoodCombo, Long> {

    Optional<FoodCombo> findByNameIgnoreCase(String name);

    List<FoodCombo> findByStatus(FoodItemStatus status);

    Page<FoodCombo> findByStatus(FoodItemStatus status, Pageable pageable);

    List<FoodCombo> findByStatusIn(Collection<FoodItemStatus> statuses);

    Page<FoodCombo> findByStatusIn(Collection<FoodItemStatus> statuses, Pageable pageable);
}
