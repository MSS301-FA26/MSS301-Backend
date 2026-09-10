package com.sba301.cinemaai.scheduler;

import com.sba301.cinemaai.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.booking.hold-cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SeatHoldCleanupScheduler {

    private final BookingService bookingService;

    @Scheduled(fixedDelayString = "${app.booking.hold-cleanup.fixed-delay-ms:60000}")
    public void cleanupExpiredHolds() {
        int expiredCount = bookingService.releaseExpiredHolds();
        if (expiredCount > 0) {
            log.info("Released {} expired seat hold booking(s)", expiredCount);
        }
    }
}
