package com.cinemaai.payment.event;

import java.time.LocalDateTime;

public record CustomerActivatedEvent(
        String eventId,
        Long userId,
        LocalDateTime occurredAt,
        String correlationId
) {}
