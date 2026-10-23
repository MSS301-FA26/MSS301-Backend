package com.cinemaai.booking.security;

import com.cinemaai.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("bookingSecurity")
@RequiredArgsConstructor
public class BookingSecurity {
    private final BookingRepository bookingRepository;

    public boolean isOwner(Long bookingId, Long userId) {
        if (bookingId == null || userId == null) return false;
        return bookingRepository.findByIdAndUserId(bookingId, userId).isPresent();
    }
}
