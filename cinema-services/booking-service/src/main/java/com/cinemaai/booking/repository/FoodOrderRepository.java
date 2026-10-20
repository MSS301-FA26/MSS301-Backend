package com.cinemaai.booking.repository;

import com.cinemaai.booking.entity.FoodOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {
    Page<FoodOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<FoodOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<FoodOrder> findByIdAndUserId(Long id, Long userId);
    List<FoodOrder> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
    Optional<FoodOrder> findByFoodOrderCode(String code);
}