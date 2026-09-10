package com.sba301.cinemaai.scheduler;

import com.sba301.cinemaai.service.FoodOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.food-order.payment-cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FoodOrderPaymentCleanupScheduler {

    private final FoodOrderService foodOrderService;

    @Scheduled(fixedDelayString = "${app.food-order.payment-cleanup.fixed-delay-ms:60000}")
    public void expirePendingPayments() {
        int count = foodOrderService.expirePendingOrders();
        if (count > 0) {
            log.info("Expired {} unpaid food order(s)", count);
        }
    }
}
