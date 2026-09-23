package com.cinemaai.booking.repository;

import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.enums.BookingSeatStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    @Query(value = """
        SELECT bs.seat_id FROM booking_seats bs
        JOIN bookings b ON bs.booking_id = b.id
        WHERE bs.showtime_id = :showtimeId
          AND bs.seat_id IN (:seatIds)
          AND (bs.status = 'BOOKED' OR (bs.status = 'HOLDING' AND b.hold_expires_at > :now))
        FOR UPDATE
    """, nativeQuery = true)
    List<Long> findConflictedSeatIds(
            @Param("showtimeId") Long showtimeId,
            @Param("seatIds") Collection<Long> seatIds,
            @Param("now") LocalDateTime now
    );

    List<BookingSeat> findByShowtimeIdAndStatusIn(
            Long showtimeId, Collection<BookingSeatStatus> statuses);

    List<BookingSeat> findByBookingId(Long bookingId);
}
