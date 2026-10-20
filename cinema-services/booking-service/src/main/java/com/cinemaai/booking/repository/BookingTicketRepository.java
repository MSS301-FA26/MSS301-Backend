package com.cinemaai.booking.repository;

import com.cinemaai.booking.entity.BookingTicket;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingTicketRepository extends JpaRepository<BookingTicket, Long> {
    List<BookingTicket> findByBookingId(Long bookingId);
}
