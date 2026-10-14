package com.cinemaai.identity.service;

import com.cinemaai.identity.repository.CustomerActivationOutboxRepository;
import com.cinemaai.identity.config.CustomerActivationRabbitConfig;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerActivationOutboxPublisher {
    private final CustomerActivationOutboxRepository repository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    @Scheduled(fixedDelayString = "${customer-activation.outbox.publish-delay-ms:2000}")
    public void publishPending() {
        // SKIP LOCKED keeps separate publisher instances from sending the same row concurrently.
        // The DB lock lasts through confirmation; a crash after ACK before commit can replay safely.
        for (var event : repository.lockPending(LocalDateTime.now(), 10)) {
            CorrelationData correlation = new CorrelationData(event.getEventId());
            try {
                rabbitTemplate.convertAndSend(CustomerActivationRabbitConfig.IDENTITY_EXCHANGE,
                        CustomerActivationRabbitConfig.CUSTOMER_ACTIVATED_ROUTING_KEY, event.getPayload(),
                        message -> {
                            message.getMessageProperties().setMessageId(event.getEventId());
                            message.getMessageProperties().setHeader("eventType", event.getEventType());
                            return message;
                        }, correlation);
                // Returned-message metadata is populated before the confirmation future completes:
                // https://docs.spring.io/spring-amqp/reference/3.1/amqp/template.html
                CorrelationData.Confirm confirm = correlation.getFuture().get(3, TimeUnit.SECONDS);
                if (!confirm.isAck() || correlation.getReturned() != null) {
                    throw new IllegalStateException("Activation delivery not confirmed and routed");
                }
                event.markPublished(LocalDateTime.now());
                log.info("event=customer_activation_published eventId={} userId={}", event.getEventId(), event.getAggregateId());
            } catch (Exception failure) {
                event.defer(LocalDateTime.now());
                log.warn("event=customer_activation_publish_failed eventId={} userId={} retryCount={} errorType={}",
                        event.getEventId(), event.getAggregateId(), event.getRetryCount(), failure.getClass().getSimpleName());
                if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
                // Bound an offline-broker transaction to one failed send, not ten timeouts.
                break;
            }
        }
    }
}
