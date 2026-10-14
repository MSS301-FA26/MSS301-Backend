package com.cinemaai.identity.repository;

import com.cinemaai.identity.entity.CustomerActivationOutboxEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerActivationOutboxRepository extends JpaRepository<CustomerActivationOutboxEvent, Long> {
    // PostgreSQL unique arbitration, not check-then-insert:
    // https://www.postgresql.org/docs/17/sql-insert.html#SQL-ON-CONFLICT
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO outbox_events
                (event_id, aggregate_type, aggregate_id, event_type, payload,
                 status, retry_count, created_at, next_attempt_at)
            VALUES (:eventId, 'USER', :userId, 'CUSTOMER_ACTIVATED', :payload,
                    'PENDING', 0, :now, :now)
            ON CONFLICT (aggregate_type, aggregate_id, event_type) DO NOTHING
            """, nativeQuery = true)
    int insertActivationIfAbsent(@Param("eventId") String eventId, @Param("userId") Long userId,
                                 @Param("payload") String payload, @Param("now") LocalDateTime now);

    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING' AND event_type = 'CUSTOMER_ACTIVATED' AND next_attempt_at <= :now
            ORDER BY id LIMIT :batchSize FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<CustomerActivationOutboxEvent> lockPending(@Param("now") LocalDateTime now,
                                                   @Param("batchSize") int batchSize);
}
