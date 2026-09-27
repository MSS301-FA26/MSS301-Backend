package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.FoodItem;
import com.cinemaai.catalog.enums.FoodItemStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    Optional<FoodItem> findByNameIgnoreCase(String name);

    List<FoodItem> findByStatus(FoodItemStatus status);

    Page<FoodItem> findByStatus(FoodItemStatus status, Pageable pageable);

    List<FoodItem> findByStatusIn(Collection<FoodItemStatus> statuses);

    Page<FoodItem> findByStatusIn(Collection<FoodItemStatus> statuses, Pageable pageable);
}
