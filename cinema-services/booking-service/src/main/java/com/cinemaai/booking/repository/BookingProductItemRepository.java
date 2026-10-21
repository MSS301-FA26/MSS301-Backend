package com.cinemaai.booking.repository;

import com.cinemaai.booking.entity.BookingProductItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingProductItemRepository extends JpaRepository<BookingProductItem, Long> {
    List<BookingProductItem> findByBookingId(Long bookingId);
}
