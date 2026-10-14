package com.cinemaai.identity.event;

import java.time.LocalDateTime;

/** Internal identifier and delivery metadata only; never credentials or profile data. */
public record CustomerActivatedEvent(
        String eventId, Long userId, LocalDateTime occurredAt, String correlationId) {
}
