package com.cinemaai.booking.repository;

import com.cinemaai.booking.entity.TicketAuditLog;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketAuditLogRepository extends JpaRepository<TicketAuditLog, Long> {

    List<TicketAuditLog> findByBookingId(Long bookingId);

    Page<TicketAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<TicketAuditLog> findByCinemaIdOrderByCreatedAtDesc(Long cinemaId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM TicketAuditLog t
            WHERE (:cinemaId IS NULL OR t.cinemaId = :cinemaId)
              AND t.action = :action
              AND t.status = :status
            """)
    java.math.BigDecimal sumAmountByCinemaIdAndActionAndStatus(
            @org.springframework.data.repository.query.Param("cinemaId") Long cinemaId,
            @org.springframework.data.repository.query.Param("action") String action,
            @org.springframework.data.repository.query.Param("status") String status);
}
