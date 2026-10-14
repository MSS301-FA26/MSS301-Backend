package com.cinemaai.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor
public class CustomerActivationOutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;
    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "retry_count", nullable = false)
    private int retryCount;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    public void markPublished(LocalDateTime now) {
        status = "PUBLISHED";
        publishedAt = now;
    }

    public void defer(LocalDateTime now) {
        // Never abandon an activation after an outage; retain it for replay.
        retryCount = Math.min(retryCount, Integer.MAX_VALUE - 1) + 1;
        long delaySeconds = Math.min(300L, 1L << Math.min(retryCount, 8));
        nextAttemptAt = now.plusSeconds(delaySeconds);
    }
}
