package com.cinemaai.identity.service;

import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.UserStatus;
import com.cinemaai.identity.event.CustomerActivatedEvent;
import com.cinemaai.identity.repository.CustomerActivationOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerActivationOutboxService {
    private static final Pattern SAFE_CORRELATION = Pattern.compile("[A-Za-z0-9_.:-]{1,128}");
    private final CustomerActivationOutboxRepository repository;
    private final UserRoleService userRoleService;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordActivation(User user) {
        if (!user.isEmailVerified() || user.getStatus() != UserStatus.ACTIVE) return;
        if (user.getId() == null || user.getId() <= 0) {
            throw new IllegalArgumentException("Activated customer must have an internal user ID");
        }
        if (!userRoleService.getRoleNames(user.getId()).contains("CUSTOMER")) return;

        String eventId = UUID.randomUUID().toString();
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || !SAFE_CORRELATION.matcher(correlationId).matches()) correlationId = eventId;
        LocalDateTime now = LocalDateTime.now();
        String payload;
        try {
            payload = objectMapper.writeValueAsString(new CustomerActivatedEvent(eventId, user.getId(), now, correlationId));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize customer activation event", e);
        }
        int inserted = repository.insertActivationIfAbsent(eventId, user.getId(), payload, now);
        log.info("event=customer_activation_outbox_recorded userId={} eventId={} correlationId={} inserted={}",
                user.getId(), eventId, correlationId, inserted == 1);
    }
}
