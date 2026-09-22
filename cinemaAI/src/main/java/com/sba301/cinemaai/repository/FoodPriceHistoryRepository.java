package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.FoodPriceHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodPriceHistoryRepository extends JpaRepository<FoodPriceHistory, Long> {

    List<FoodPriceHistory> findByFoodItemIdOrderByChangedAtDesc(Long foodItemId);

    List<FoodPriceHistory> findByFoodComboIdOrderByChangedAtDesc(Long foodComboId);
}
