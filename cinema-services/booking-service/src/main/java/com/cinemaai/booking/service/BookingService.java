package com.cinemaai.booking.service;

import com.cinemaai.booking.dto.request.CheckoutBookingRequest;
import com.cinemaai.booking.dto.request.HoldSeatsRequest;
import com.cinemaai.booking.dto.request.UpdateHoldingBookingRequest;
import com.cinemaai.booking.dto.response.BookingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {

    BookingResponse holdSeats(Long userId, HoldSeatsRequest request);

    BookingResponse checkout(Long userId, Long bookingId, CheckoutBookingRequest request);

    BookingResponse updateHoldingItems(Long userId, Long bookingId, UpdateHoldingBookingRequest request);

    BookingResponse cancelBooking(Long userId, Long bookingId);

    Page<BookingResponse> getMyBookings(Long userId, Pageable pageable);

    BookingResponse getBookingById(Long userId, Long bookingId);

    BookingResponse getBookingByIdInternal(Long bookingId);

    BookingResponse getBookingByCode(String bookingCode);
}
