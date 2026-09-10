package com.sba301.cinemaai.scheduler;

import com.sba301.cinemaai.repository.LoyaltyConfigurationRepository;
import com.sba301.cinemaai.service.LoyaltyPointService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoyaltyPointExpiryScheduler {

    private final LoyaltyConfigurationRepository loyaltyConfigurationRepository;
    private final LoyaltyPointService loyaltyPointService;

    @Value("${cinema.loyalty.expiry-check-enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${cinema.loyalty.expiry-check-delay-ms:60000}")
    public void expirePointsIfDue() {
        if (!enabled) {
            return;
        }
        loyaltyConfigurationRepository.findTopByOrderByIdAsc().ifPresent(config -> {
            LocalDate today = LocalDate.now();
            if (config.getLastExpiredAt() != null && config.getLastExpiredAt().isEqual(today)) {
                return;
            }
            if (today.getMonthValue() != config.getExpiryMonth() || today.getDayOfMonth() != config.getExpiryDay()) {
                return;
            }
            LocalTime configuredTime = LocalTime.parse(config.getExpiryTime());
            if (LocalDateTime.now().toLocalTime().isBefore(configuredTime)) {
                return;
            }
            int affectedAccounts = loyaltyPointService.expireAllActivePoints("SYSTEM");
            log.info("Loyalty expiry job reset points for {} account(s)", affectedAccounts);
        });
    }
}
