package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.FoodComboItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodComboItemRepository extends JpaRepository<FoodComboItem, Long> {

    @Query("SELECT fci FROM FoodComboItem fci JOIN FETCH fci.foodItem WHERE fci.combo.id = :comboId")
    List<FoodComboItem> findByComboIdWithItems(Long comboId);

    List<FoodComboItem> findByComboId(Long comboId);

    @Modifying
    @Query("DELETE FROM FoodComboItem fci WHERE fci.combo.id = :comboId")
    void deleteByComboId(Long comboId);

    boolean existsByFoodItemId(Long foodItemId);
}
