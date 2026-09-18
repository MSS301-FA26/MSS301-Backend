package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.FoodInventoryTransaction;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodInventoryTransactionRepository extends JpaRepository<FoodInventoryTransaction, Long> {

    List<FoodInventoryTransaction> findByCinemaIdAndFoodItemIdOrderByCreatedAtDesc(Long cinemaId, Long foodItemId);

    List<FoodInventoryTransaction> findByFoodItemIdOrderByCreatedAtDesc(Long foodItemId);

    Page<FoodInventoryTransaction> findByCinemaIdOrderByCreatedAtDesc(Long cinemaId, Pageable pageable);

    Page<FoodInventoryTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
