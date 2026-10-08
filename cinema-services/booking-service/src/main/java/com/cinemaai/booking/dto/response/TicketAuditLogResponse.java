package com.cinemaai.booking.dto.response;

import com.cinemaai.booking.entity.TicketAuditLog;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketAuditLogResponse(
        Long id,
        Long bookingId,
        String ticketCode,
        Long cinemaId,
        Long actorUserId,
        String actorEmail,
        String actorRole,
        String action,
        BigDecimal amount,
        String reason,
        String status,
        LocalDateTime createdAt
) {
    public static TicketAuditLogResponse fromEntity(TicketAuditLog entity) {
        if (entity == null) return null;
        return new TicketAuditLogResponse(
                entity.getId(),
                entity.getBookingId(),
                entity.getTicketCode(),
                entity.getCinemaId(),
                entity.getActorUserId(),
                entity.getActorEmail(),
                entity.getActorRole(),
                entity.getAction(),
                entity.getAmount(),
                entity.getReason(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
