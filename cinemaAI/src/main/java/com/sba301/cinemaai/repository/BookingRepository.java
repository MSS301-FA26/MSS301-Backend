package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.BookingStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByBookingCode(String bookingCode);

    Optional<Booking> findByBookingCode(String bookingCode);

    List<Booking> findByUser(User user);

    @EntityGraph(attributePaths = {"user.profile", "showtime.movie", "showtime.room", "showtime.room.cinema"})
    Page<Booking> findByUser(User user, Pageable pageable);

    @EntityGraph(attributePaths = {"user.profile", "showtime.movie", "showtime.room", "showtime.room.cinema"})
    List<Booking> findByShowtime(Showtime showtime);

    @EntityGraph(attributePaths = {"user.profile", "showtime.movie", "showtime.room", "showtime.room.cinema"})
    Optional<Booking> findWithDetailsById(Long id);

    /** Admin list without status filter — findAll(Pageable) cannot carry an entity graph. */
    @EntityGraph(attributePaths = {"user.profile", "showtime.movie", "showtime.room", "showtime.room.cinema"})
    @Query("SELECT b FROM Booking b")
    Page<Booking> findAllWithDetails(Pageable pageable);

    boolean existsByShowtimeAndStatusIn(Showtime showtime, Collection<BookingStatus> statuses);

    boolean existsByUserAndShowtimeAndStatusIn(
            User user,
            Showtime showtime,
            Collection<BookingStatus> statuses
    );

    List<Booking> findByStatus(BookingStatus status);

    @EntityGraph(attributePaths = {"user.profile", "showtime.movie", "showtime.room", "showtime.room.cinema"})
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    Page<Booking> findByStatusAndBulkRefundTrue(BookingStatus status, Pageable pageable);

    List<Booking> findTop50ByStatusAndBulkRefundTrueOrderByRefundRequestedAtAsc(BookingStatus status);

    List<Booking> findByStatusAndHoldExpiresAtBefore(BookingStatus status, LocalDateTime expiresAt);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b WHERE b.user = :user AND b.showtime.movie = :movie AND b.status = 'USED'")
    boolean existsUsedBookingByUserAndMovie(@Param("user") User user, @Param("movie") Movie movie);

    @Query("SELECT b FROM Booking b WHERE b.user = :user AND b.showtime.movie = :movie AND b.status = 'USED' ORDER BY b.checkedInAt DESC, b.id DESC")
    List<Booking> findUsedBookingsByUserAndMovieOrderByLatest(@Param("user") User user, @Param("movie") Movie movie);

    @Query("SELECT b FROM Booking b WHERE b.showtime = :showtime AND b.status = :status")
    List<Booking> findByShowtimeAndStatus(@Param("showtime") Showtime showtime, @Param("status") BookingStatus status);

    @EntityGraph(attributePaths = {"user.profile", "showtime.movie", "showtime.room", "showtime.room.cinema"})
    @Query("""
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
    List<Booking> findRecentForCheckIn(@Param("statuses") Collection<BookingStatus> statuses, Pageable pageable);

    @Query("SELECT COUNT(bs) FROM BookingSeat bs JOIN bs.booking b WHERE b.status IN ('PAID','USED') AND b.paidAt BETWEEN :from AND :to")
    long countTicketsSold(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT b.showtime.movie.id, b.showtime.movie.title, COUNT(bs), SUM(b.totalAmount)
            FROM BookingSeat bs JOIN bs.booking b
            WHERE b.status IN ('PAID','USED') AND b.paidAt BETWEEN :from AND :to
            GROUP BY b.showtime.movie.id, b.showtime.movie.title
            ORDER BY COUNT(bs) DESC
            """)
    List<Object[]> topMoviesByTickets(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Lấp đầy theo phòng: tử số = ghế bán của các SUẤT trong khoảng thời gian,
    // mẫu số = sức chứa phòng × số suất (không CANCELLED) — tách 2 query rồi merge ở service.
    @Query("""
            SELECT s.room.id, s.room.name, COUNT(bs)
            FROM BookingSeat bs JOIN bs.showtime s
            WHERE bs.booking.status IN ('PAID','USED') AND s.startTime BETWEEN :from AND :to
            GROUP BY s.room.id, s.room.name
            """)
    List<Object[]> soldSeatsByRoom(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT st.room.id, st.room.name, COUNT(st), (st.room.rowCount * st.room.columnCount)
            FROM Showtime st
            WHERE st.startTime BETWEEN :from AND :to AND st.status <> 'CANCELLED'
            GROUP BY st.room.id, st.room.name, st.room.rowCount, st.room.columnCount
            """)
    List<Object[]> roomShowCapacity(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);


    @Query("""
            SELECT s.id, s.movie.title, s.room.name, s.startTime,
                   (s.room.rowCount * s.room.columnCount),
                   (SELECT COUNT(bs) FROM BookingSeat bs
                    WHERE bs.showtime = s AND bs.booking.status IN ('PAID','USED'))
            FROM Showtime s
            WHERE s.startTime BETWEEN :from AND :to AND s.status <> 'CANCELLED'
            ORDER BY s.startTime ASC
            """)
    List<Object[]> showtimeFill(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'PAID' AND b.showtime.endTime < :now AND b.paidAt BETWEEN :from AND :to")
    long countNoShows(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status IN ('PAID','USED') AND b.showtime.endTime < :now AND b.paidAt BETWEEN :from AND :to")
    long countFinishedPaidBookings(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("now") LocalDateTime now);

    @Query("""
            SELECT EXTRACT(HOUR FROM b.showtime.startTime), COUNT(bs)
            FROM BookingSeat bs JOIN bs.booking b
            WHERE b.status IN ('PAID','USED') AND b.paidAt BETWEEN :from AND :to
            GROUP BY EXTRACT(HOUR FROM b.showtime.startTime)
            ORDER BY EXTRACT(HOUR FROM b.showtime.startTime)
            """)
    List<Object[]> ticketsByHourOfDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT bs.seat.rowLabel, bs.seat.seatNumber, bs.seat.seatType, COUNT(bs)
            FROM BookingSeat bs JOIN bs.booking b
            WHERE b.status IN ('PAID','USED') AND b.paidAt BETWEEN :from AND :to
              AND (:roomId IS NULL OR bs.seat.room.id = :roomId)
            GROUP BY bs.seat.rowLabel, bs.seat.seatNumber, bs.seat.seatType
            ORDER BY COUNT(bs) DESC
            """)
    List<Object[]> topSeats(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                            @Param("roomId") Long roomId);

    @Query("SELECT b.status, COUNT(b) FROM Booking b WHERE b.createdAt BETWEEN :from AND :to GROUP BY b.status")
    List<Object[]> countByStatusInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT b.user.id, b.user.email, COUNT(b), SUM(b.totalAmount)
            FROM Booking b
            WHERE b.status = 'EXPIRED' AND b.createdAt BETWEEN :from AND :to
            GROUP BY b.user.id, b.user.email
            ORDER BY COUNT(b) DESC
            """)
    List<Object[]> expiredBookingsByUser(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Lock(LockModeType.PESSIMISTIC_WRITE) // Sinh lệnh khóa dòng dữ liệu trong SQL Server
    @Query("SELECT b FROM Booking b WHERE b.showtime.id = :showtimeId AND b.status = :status")
    List<Booking> findByShowtimeIdAndStatusForUpdate(
            @Param("showtimeId") Long showtimeId,
            @Param("status") BookingStatus status
    );

    // Tìm và KHÓA nhiều trạng thái cùng lúc (dùng cho bulk refund khi hủy suất chiếu)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.showtime.id = :showtimeId AND b.status IN :statuses")
    List<Booking> findByShowtimeIdAndStatusInForUpdate(
            @Param("showtimeId") Long showtimeId,
            @Param("statuses") List<BookingStatus> statuses
    );

    // 2. Hàm tìm và KHÓA ĐƠN LẺ (Dùng cho Staff khi bấm duyệt xử lý lỗi thủ công offline)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}
