package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.FoodInventory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodInventoryRepository extends JpaRepository<FoodInventory, Long> {

    Optional<FoodInventory> findByCinemaIdAndFoodItemId(Long cinemaId, Long foodItemId);

    List<FoodInventory> findByFoodItemId(Long foodItemId);

    List<FoodInventory> findByCinemaId(Long cinemaId);

    @Query("SELECT COALESCE(SUM(fi.quantity - fi.reservedQuantity), 0) FROM FoodInventory fi WHERE fi.foodItem.id = :foodItemId")
    int getTotalAvailableStock(@Param("foodItemId") Long foodItemId);

    @Query("SELECT COUNT(fi) FROM FoodInventory fi WHERE fi.cinema.id = :cinemaId AND (fi.quantity - fi.reservedQuantity) <= 0")
    long countOutOfStockByCinemaId(@Param("cinemaId") Long cinemaId);

    @Query("SELECT COUNT(fi) FROM FoodInventory fi WHERE fi.cinema.id = :cinemaId AND (fi.quantity - fi.reservedQuantity) > 0 AND (fi.quantity - fi.reservedQuantity) <= fi.lowStockThreshold")
    long countLowStockByCinemaId(@Param("cinemaId") Long cinemaId);

    @Query("SELECT COUNT(fi) FROM FoodInventory fi WHERE (fi.quantity - fi.reservedQuantity) <= 0")
    long countTotalOutOfStock();

    @Query("SELECT COUNT(fi) FROM FoodInventory fi WHERE (fi.quantity - fi.reservedQuantity) > 0 AND (fi.quantity - fi.reservedQuantity) <= fi.lowStockThreshold")
    long countTotalLowStock();
}
