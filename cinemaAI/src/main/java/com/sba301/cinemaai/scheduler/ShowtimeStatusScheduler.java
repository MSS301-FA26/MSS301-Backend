package com.sba301.cinemaai.scheduler;

import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.enums.ShowtimeStatus;
import com.sba301.cinemaai.repository.ShowtimeRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Automatically transitions showtime statuses:
 * SCHEDULED → OPEN  : when startTime has arrived
 * OPEN      → COMPLETED : when endTime has passed
 *
 * Runs every minute by default; disabled via app.showtime.scheduler.enabled=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.showtime.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ShowtimeStatusScheduler {

    private final ShowtimeRepository showtimeRepository;

    @Scheduled(fixedDelayString = "${app.showtime.scheduler.fixed-delay-ms:60000}")
    @Transactional
    public void updateShowtimeStatuses() {
        LocalDateTime now = LocalDateTime.now();

        List<Showtime> toOpen = showtimeRepository.findScheduledReadyToOpen(now);
        for (Showtime showtime : toOpen) {
            showtime.setStatus(ShowtimeStatus.OPEN);
            log.info("Showtime {} transitioned SCHEDULED → OPEN (startTime={})", showtime.getId(), showtime.getStartTime());
        }
        if (!toOpen.isEmpty()) {
            showtimeRepository.saveAll(toOpen);
        }

        List<Showtime> toComplete = showtimeRepository.findOpenReadyToComplete(now);
        for (Showtime showtime : toComplete) {
            showtime.setStatus(ShowtimeStatus.COMPLETED);
            log.info("Showtime {} transitioned OPEN → COMPLETED (endTime={})", showtime.getId(), showtime.getEndTime());
        }
        if (!toComplete.isEmpty()) {
            showtimeRepository.saveAll(toComplete);
        }

        if (!toOpen.isEmpty() || !toComplete.isEmpty()) {
            log.info("Showtime scheduler: {} opened, {} completed", toOpen.size(), toComplete.size());
        }
    }
}
