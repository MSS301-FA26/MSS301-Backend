package com.cinemaai.payment.listener;

import com.cinemaai.payment.config.RabbitMqConfig;
import com.cinemaai.payment.event.CustomerActivatedEvent;
import com.cinemaai.payment.service.WalletProvisioningService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerActivatedEventListener {

    private final ObjectMapper objectMapper;
    private final WalletProvisioningService provisioningService;

    @RabbitListener(queues = RabbitMqConfig.CUSTOMER_ACTIVATED_QUEUE,
            containerFactory = "customerActivationListenerContainerFactory")
    public void onCustomerActivated(String payload) {
        CustomerActivatedEvent event;
        try {
            event = objectMapper.readValue(payload, CustomerActivatedEvent.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            log.warn("event=wallet_provisioning_rejected entryPoint=customer_activation_consumer errorType={}",
                    exception.getClass().getSimpleName());
            throw new AmqpRejectAndDontRequeueException("Invalid customer activation event");
        }
        if (event == null || !safeIdentifier(event.eventId()) || event.userId() == null
                || event.userId() <= 0 || event.occurredAt() == null
                || (event.correlationId() != null && !safeIdentifier(event.correlationId()))) {
            log.warn("event=wallet_provisioning_rejected entryPoint=customer_activation_consumer reason=invalid_contract");
            throw new AmqpRejectAndDontRequeueException("Invalid customer activation event");
        }

        log.info("event=wallet_provisioning_requested entryPoint=customer_activation_consumer eventId={} userId={} correlationId={}",
                event.eventId(), event.userId(), event.correlationId());
        try {
            boolean created = provisioningService.ensureWalletExists(event.userId());
            log.info("event={} entryPoint=customer_activation_consumer eventId={} userId={} correlationId={}",
                    created ? "wallet_provisioned" : "wallet_provisioning_already_exists",
                    event.eventId(), event.userId(), event.correlationId());
        } catch (RuntimeException exception) {
            log.warn("event=wallet_provisioning_failed entryPoint=customer_activation_consumer eventId={} userId={} correlationId={} errorType={}",
                    event.eventId(), event.userId(), event.correlationId(), exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private boolean safeIdentifier(String value) {
        return value != null && value.matches("[A-Za-z0-9_.:-]{1,128}");
    }
}
