package com.cinemaai.booking.service.impl;

import com.cinemaai.booking.client.dto.UserAccessScopeDto;
import com.cinemaai.booking.dto.request.AdminCancelTicketRequest;
import com.cinemaai.booking.dto.request.AdminRefundTicketRequest;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.dto.response.CinemaDashboardResponse;
import com.cinemaai.booking.dto.response.TicketAuditLogResponse;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.entity.TicketAuditLog;
import com.cinemaai.booking.enums.BookingSeatStatus;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.exception.ConflictException;
import com.cinemaai.booking.exception.NotFoundException;
import com.cinemaai.booking.mapper.BookingMapper;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.TicketAuditLogRepository;
import com.cinemaai.booking.security.AuthenticatedUser;
import com.cinemaai.booking.security.CinemaSecurityService;
import com.cinemaai.booking.service.AdminBookingService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBookingServiceImpl implements AdminBookingService {

    private final BookingRepository bookingRepository;
    private final TicketAuditLogRepository ticketAuditLogRepository;
    private final CinemaSecurityService cinemaSecurityService;

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookings(AuthenticatedUser actor, Long requestedCinemaId, BookingStatus status, String search, Pageable pageable) {
        Long enforcedCinemaId = cinemaSecurityService.resolveEnforcedCinemaId(actor, requestedCinemaId, false);
        return bookingRepository.findBookingsForAdmin(enforcedCinemaId, status, search, pageable)
                .map(BookingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(AuthenticatedUser actor, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn đặt vé #" + bookingId));

        cinemaSecurityService.validateCinemaAccess(actor, booking.getCinemaId(), false);
        return BookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(AuthenticatedUser actor, Long bookingId, AdminCancelTicketRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn đặt vé #" + bookingId));

        cinemaSecurityService.validateCinemaAccess(actor, booking.getCinemaId(), false);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Vé đã bị hủy trước đó.");
        }
        if (booking.getStatus() == BookingStatus.REFUNDED) {
            throw new BadRequestException("Vé đã được hoàn tiền, không thể thực hiện hủy.");
        }
        if (booking.getStatus() == BookingStatus.USED) {
            throw new ConflictException("Không thể hủy vé đã được check-in sử dụng dịch vụ.");
        }

        LocalDateTime now = LocalDateTime.now();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);

        if (booking.getSeats() != null) {
            for (BookingSeat seat : booking.getSeats()) {
                seat.setStatus(BookingSeatStatus.RELEASED);
            }
        }
        bookingRepository.save(booking);

        UserAccessScopeDto scope = cinemaSecurityService.getAuthoritativeScope(actor);
        String actorRole = (scope.roles() != null && scope.roles().stream().anyMatch(r -> r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("ROLE_ADMIN")))
                ? "ADMIN" : "MANAGER";

        BigDecimal finalAmount = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
        TicketAuditLog auditLog = TicketAuditLog.builder()
                .bookingId(booking.getId())
                .ticketCode(booking.getBookingCode())
                .cinemaId(booking.getCinemaId())
                .actorUserId(actor.id())
                .actorEmail(actor.email())
                .actorRole(actorRole)
                .action("CANCEL")
                .amount(finalAmount)
                .reason(request.reason())
                .status("SUCCESS")
                .build();
        ticketAuditLogRepository.save(auditLog);

        log.info("Ticket #{} cancelled by user {} with role {}", booking.getBookingCode(), actor.id(), actorRole);
        return BookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse refundBooking(AuthenticatedUser actor, Long bookingId, AdminRefundTicketRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn đặt vé #" + bookingId));

        cinemaSecurityService.validateCinemaAccess(actor, booking.getCinemaId(), false);

        if (booking.getStatus() == BookingStatus.REFUNDED) {
            throw new ConflictException("Vé này đã được hoàn tiền trước đó. Hệ thống ngăn chặn việc hoàn tiền trùng lặp.");
        }
        if (booking.getStatus() == BookingStatus.USED) {
            throw new ConflictException("Không thể hoàn tiền cho vé đã được check-in vào rạp xem phim.");
        }
        if (booking.getStatus() == BookingStatus.HOLDING || booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Vé chưa được thanh toán, không phát sinh số tiền nào cần hoàn.");
        }

        LocalDateTime now = LocalDateTime.now();
        booking.setStatus(BookingStatus.REFUNDED);
        booking.setRefundedAt(now);
        booking.setRefundReason(request.reason());

        if (booking.getSeats() != null) {
            for (BookingSeat seat : booking.getSeats()) {
                seat.setStatus(BookingSeatStatus.RELEASED);
            }
        }
        bookingRepository.save(booking);

        UserAccessScopeDto scope = cinemaSecurityService.getAuthoritativeScope(actor);
        String actorRole = (scope.roles() != null && scope.roles().stream().anyMatch(r -> r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("ROLE_ADMIN")))
                ? "ADMIN" : "MANAGER";

        BigDecimal finalAmount = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
        TicketAuditLog auditLog = TicketAuditLog.builder()
                .bookingId(booking.getId())
                .ticketCode(booking.getBookingCode())
                .cinemaId(booking.getCinemaId())
                .actorUserId(actor.id())
                .actorEmail(actor.email())
                .actorRole(actorRole)
                .action("REFUND")
                .amount(finalAmount)
                .reason(request.reason())
                .status("SUCCESS")
                .build();
        ticketAuditLogRepository.save(auditLog);

        log.info("Ticket #{} refunded by user {} with role {}", booking.getBookingCode(), actor.id(), actorRole);
        return BookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketAuditLogResponse> getAuditLogs(AuthenticatedUser actor, Long requestedCinemaId, Pageable pageable) {
        Long enforcedCinemaId = cinemaSecurityService.resolveEnforcedCinemaId(actor, requestedCinemaId, false);
        if (enforcedCinemaId != null) {
            return ticketAuditLogRepository.findByCinemaIdOrderByCreatedAtDesc(enforcedCinemaId, pageable)
                    .map(TicketAuditLogResponse::fromEntity);
        }
        return ticketAuditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(TicketAuditLogResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaDashboardResponse getDashboardMetrics(AuthenticatedUser actor, Long requestedCinemaId) {
        Long enforcedCinemaId = cinemaSecurityService.resolveEnforcedCinemaId(actor, requestedCinemaId, false);

        List<BookingStatus> paidStatuses = List.of(BookingStatus.PAID, BookingStatus.USED);

        // 1. Total revenue strictly from PAID and USED tickets (unpaid HOLDING/PENDING_PAYMENT are excluded)
        BigDecimal totalRevenue = bookingRepository.sumRevenueByCinemaIdAndStatusIn(enforcedCinemaId, paidStatuses);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        // 2. Counts
        long totalPaidTickets = bookingRepository.countByCinemaIdAndStatusIn(enforcedCinemaId, paidStatuses);
        long totalCancelledTickets = bookingRepository.countByCinemaIdAndStatusIn(enforcedCinemaId, List.of(BookingStatus.CANCELLED));
        long totalRefundedTickets = bookingRepository.countByCinemaIdAndStatusIn(enforcedCinemaId, List.of(BookingStatus.REFUNDED));

        // 3. Total refunded amount from successful refund audit logs
        BigDecimal totalRefundedAmount = ticketAuditLogRepository.sumAmountByCinemaIdAndActionAndStatus(enforcedCinemaId, "REFUND", "SUCCESS");
        if (totalRefundedAmount == null) totalRefundedAmount = BigDecimal.ZERO;

        // 4. Occupancy Rate estimation
        long totalAttempts = totalPaidTickets + totalCancelledTickets;
        double occupancyRate = totalAttempts > 0 ? ((double) totalPaidTickets / totalAttempts) * 100.0 : 0.0;
        occupancyRate = Math.round(occupancyRate * 10.0) / 10.0;

        // 5. Cinema Metrics Breakdown
        List<CinemaDashboardResponse.CinemaMetricDto> cinemaMetrics = new ArrayList<>();
        if (enforcedCinemaId == null) {
            List<Object[]> rawBreakdown = bookingRepository.findRevenueByCinemaGroupByCinemaId(paidStatuses);
            for (Object[] row : rawBreakdown) {
                Long cid = (Long) row[0];
                long count = (Long) row[1];
                BigDecimal rev = (BigDecimal) row[2];
                cinemaMetrics.add(new CinemaDashboardResponse.CinemaMetricDto(cid, count, rev));
            }
        } else {
            cinemaMetrics.add(new CinemaDashboardResponse.CinemaMetricDto(enforcedCinemaId, totalPaidTickets, totalRevenue));
        }

        // 6. Recent audit logs
        List<TicketAuditLogResponse> recentAuditLogs = ticketAuditLogRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5))
                .getContent()
                .stream()
                .filter(l -> enforcedCinemaId == null || enforcedCinemaId.equals(l.getCinemaId()))
                .map(TicketAuditLogResponse::fromEntity)
                .toList();

        // 7. Pending refund tickets (CANCELLED bookings that might need refund attention)
        List<BookingResponse> pendingRefund = bookingRepository
                .findBookingsForAdmin(enforcedCinemaId, BookingStatus.CANCELLED, null, PageRequest.of(0, 5))
                .getContent()
                .stream()
                .map(BookingMapper::toResponse)
                .toList();

        return new CinemaDashboardResponse(
                enforcedCinemaId,
                totalRevenue,
                totalPaidTickets,
                totalCancelledTickets,
                totalRefundedTickets,
                totalRefundedAmount,
                occupancyRate,
                cinemaMetrics,
                recentAuditLogs,
                pendingRefund
        );
    }
}
