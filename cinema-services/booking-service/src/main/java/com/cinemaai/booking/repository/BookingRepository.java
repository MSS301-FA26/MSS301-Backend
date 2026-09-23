package com.cinemaai.booking.repository;

import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.enums.BookingStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingCode(String bookingCode);

    Page<Booking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    List<Booking> findByUserIdAndShowtimeIdAndStatusIn(
            Long userId, Long showtimeId, Collection<BookingStatus> statuses);

    List<Booking> findByStatusInAndHoldExpiresAtBefore(
            Collection<BookingStatus> statuses, LocalDateTime time);
}
