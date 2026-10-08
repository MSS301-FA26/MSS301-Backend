package com.cinemaai.booking.repository;

import com.cinemaai.booking.entity.BookingFoodItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingFoodItemRepository extends JpaRepository<BookingFoodItem, Long> {
    List<BookingFoodItem> findByBookingId(Long bookingId);
}
