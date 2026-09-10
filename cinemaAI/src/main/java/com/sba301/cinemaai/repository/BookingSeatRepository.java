package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.BookingSeat;
import com.sba301.cinemaai.entity.Seat;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.enums.SeatRuntimeStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    List<BookingSeat> findByBooking(Booking booking);

    @EntityGraph(attributePaths = {"seat"})
    List<BookingSeat> findByBookingIn(Collection<Booking> bookings);

    List<BookingSeat> findByShowtime(Showtime showtime);

    Optional<BookingSeat> findByShowtimeAndSeat(Showtime showtime, Seat seat);

    List<BookingSeat> findByShowtimeAndSeatAndStatusIn(
            Showtime showtime,
            Seat seat,
            List<SeatRuntimeStatus> statuses
    );

    Optional<BookingSeat> findByTicketCode(String ticketCode);
}
