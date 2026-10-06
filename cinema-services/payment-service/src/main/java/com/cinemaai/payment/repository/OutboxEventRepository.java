package com.cinemaai.payment.repository;

import com.cinemaai.payment.entity.OutboxEvent;
import com.cinemaai.payment.enums.OutboxStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE status = :status
            ORDER BY created_at ASC
            LIMIT 50
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findNextPendingEventsForPublish(@Param("status") String status);
}
