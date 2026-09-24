package com.cinemaai.booking.scheduler;

import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.enums.BookingSeatStatus;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.repository.BookingRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldCleanupScheduler {

    private final BookingRepository bookingRepository;

    @Scheduled(fixedRateString = "${booking.hold.cleanup.fixed-delay-ms}")
    @Transactional
    public void cleanupExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingRepository.findByStatusInAndHoldExpiresAtBefore(
                List.of(BookingStatus.HOLDING, BookingStatus.PENDING_PAYMENT), now);

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("Found {} expired bookings to clean up and release seats.", expiredBookings.size());
        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setCancelledAt(now);
            if (booking.getSeats() != null) {
                for (BookingSeat seat : booking.getSeats()) {
                    seat.setStatus(BookingSeatStatus.RELEASED);
                }
            }
        }

        bookingRepository.saveAll(expiredBookings);
    }
}
