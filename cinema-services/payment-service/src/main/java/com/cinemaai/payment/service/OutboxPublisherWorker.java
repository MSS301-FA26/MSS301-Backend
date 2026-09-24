package com.cinemaai.payment.service;

import com.cinemaai.payment.config.RabbitMqConfig;
import com.cinemaai.payment.entity.OutboxEvent;
import com.cinemaai.payment.enums.OutboxStatus;
import com.cinemaai.payment.repository.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisherWorker {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.PAYMENT_EXCHANGE,
                        RabbitMqConfig.PAYMENT_SUCCEEDED_ROUTING_KEY,
                        event.getPayload()
                );
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());
                log.info("Published OutboxEvent id={}, aggregateId={} to RabbitMQ",
                        event.getId(), event.getAggregateId());
            } catch (Exception ex) {
                log.error("Failed to publish OutboxEvent id={}: {}", event.getId(), ex.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 5) {
                    event.setStatus(OutboxStatus.FAILED);
                }
            }
        }

        outboxEventRepository.saveAll(pendingEvents);
    }
}
