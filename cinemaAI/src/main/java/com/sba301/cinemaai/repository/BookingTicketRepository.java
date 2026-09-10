package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.BookingTicket;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingTicketRepository extends JpaRepository<BookingTicket, Long> {

    List<BookingTicket> findByBooking(Booking booking);

    List<BookingTicket> findByBookingIn(Collection<Booking> bookings);
}
