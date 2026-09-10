package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.FoodOrder;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.FoodOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

    List<FoodOrder> findByBooking(Booking booking);

    Optional<FoodOrder> findByOrderCode(String orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select foodOrder from FoodOrder foodOrder where foodOrder.orderCode = :orderCode")
    Optional<FoodOrder> findByOrderCodeForUpdate(@Param("orderCode") String orderCode);

    List<FoodOrder> findByCustomerOrderByCreatedAtDesc(User customer);

    List<FoodOrder> findByStatusAndExpiresAtLessThanEqual(FoodOrderStatus status, LocalDateTime expiresAt);
}
