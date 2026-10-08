package com.cinemaai.booking.repository;

import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingCode(String bookingCode);

    Page<Booking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    List<Booking> findByUserIdAndShowtimeIdAndStatusIn(
            Long userId, Long showtimeId, Collection<BookingStatus> statuses);

    List<Booking> findByUserIdAndMovieIdAndStatusIn(
            Long userId, Long movieId, Collection<BookingStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT DISTINCT b FROM Booking b
            JOIN FETCH b.seats seat
            WHERE b.showtimeId = :showtimeId
              AND b.status IN :statuses
              AND b.holdExpiresAt < :now
              AND seat.seatId IN :seatIds
            """)
    List<Booking> findExpiredHoldsForSeatsForUpdate(
            @Param("showtimeId") Long showtimeId,
            @Param("seatIds") Collection<Long> seatIds,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("now") LocalDateTime now);

    List<Booking> findByStatusInAndHoldExpiresAtBefore(
            Collection<BookingStatus> statuses, LocalDateTime time);

    @org.springframework.data.jpa.repository.Query("""
            SELECT b FROM Booking b
            WHERE b.status IN :statuses
            ORDER BY
                CASE
                    WHEN b.checkedInAt IS NOT NULL THEN b.checkedInAt
                    WHEN b.paidAt IS NOT NULL THEN b.paidAt
                    ELSE b.createdAt
                END DESC,
                b.id DESC
            """)
    List<Booking> findRecentForCheckIn(
            @org.springframework.data.repository.query.Param("statuses") Collection<BookingStatus> statuses,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            SELECT b FROM Booking b
            WHERE b.status IN :statuses
              AND (:cinemaId IS NULL OR b.cinemaId = :cinemaId)
            ORDER BY
                CASE
                    WHEN b.checkedInAt IS NOT NULL THEN b.checkedInAt
                    WHEN b.paidAt IS NOT NULL THEN b.paidAt
                    ELSE b.createdAt
                END DESC,
                b.id DESC
            """)
    List<Booking> findRecentForCheckInByCinema(
            @org.springframework.data.repository.query.Param("statuses") Collection<BookingStatus> statuses,
            @org.springframework.data.repository.query.Param("cinemaId") Long cinemaId,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
            value = """
                    SELECT b FROM Booking b
                    WHERE (:cinemaId IS NULL OR b.cinemaId = :cinemaId)
                      AND (:status IS NULL OR b.status = :status)
                    ORDER BY b.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(b) FROM Booking b
                    WHERE (:cinemaId IS NULL OR b.cinemaId = :cinemaId)
                      AND (:status IS NULL OR b.status = :status)
                    """
    )
    Page<Booking> findBookingsByCinemaAndStatus(
            @org.springframework.data.repository.query.Param("cinemaId") Long cinemaId,
            @org.springframework.data.repository.query.Param("status") BookingStatus status,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
            value = """
                    SELECT b FROM Booking b
                    WHERE (:cinemaId IS NULL OR b.cinemaId = :cinemaId)
                      AND (:status IS NULL OR b.status = :status)
                      AND (cast(:search as string) IS NULL OR LOWER(b.bookingCode) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))
                           OR LOWER(b.movieTitleSnapshot) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))
                           OR LOWER(b.cinemaNameSnapshot) LIKE LOWER(CONCAT('%', cast(:search as string), '%')))
                    ORDER BY b.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(b) FROM Booking b
                    WHERE (:cinemaId IS NULL OR b.cinemaId = :cinemaId)
                      AND (:status IS NULL OR b.status = :status)
                      AND (cast(:search as string) IS NULL OR LOWER(b.bookingCode) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))
                           OR LOWER(b.movieTitleSnapshot) LIKE LOWER(CONCAT('%', cast(:search as string), '%'))
                           OR LOWER(b.cinemaNameSnapshot) LIKE LOWER(CONCAT('%', cast(:search as string), '%')))
                    """
    )
    Page<Booking> findBookingsForAdmin(
            @org.springframework.data.repository.query.Param("cinemaId") Long cinemaId,
            @org.springframework.data.repository.query.Param("status") BookingStatus status,
            @org.springframework.data.repository.query.Param("search") String search,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE (:cinemaId IS NULL OR b.cinemaId = :cinemaId)
              AND b.status IN :statuses
            """)
    long countByCinemaIdAndStatusIn(
            @org.springframework.data.repository.query.Param("cinemaId") Long cinemaId,
            @org.springframework.data.repository.query.Param("statuses") Collection<BookingStatus> statuses);

    @org.springframework.data.jpa.repository.Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b
            WHERE (:cinemaId IS NULL OR b.cinemaId = :cinemaId)
              AND b.status IN :statuses
            """)
    java.math.BigDecimal sumRevenueByCinemaIdAndStatusIn(
            @org.springframework.data.repository.query.Param("cinemaId") Long cinemaId,
            @org.springframework.data.repository.query.Param("statuses") Collection<BookingStatus> statuses);

    @org.springframework.data.jpa.repository.Query("""
            SELECT b.cinemaId, COUNT(b), COALESCE(SUM(b.totalAmount), 0)
            FROM Booking b
            WHERE b.status IN :statuses
            GROUP BY b.cinemaId
            """)
    List<Object[]> findRevenueByCinemaGroupByCinemaId(
            @org.springframework.data.repository.query.Param("statuses") Collection<BookingStatus> statuses);

    List<Booking> findByShowtimeId(Long showtimeId);

    Page<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status, Pageable pageable);

    Page<Booking> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
