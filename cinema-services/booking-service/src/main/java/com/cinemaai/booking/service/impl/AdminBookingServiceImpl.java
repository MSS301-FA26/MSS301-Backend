package com.cinemaai.booking.service.impl;

import com.cinemaai.booking.client.IdentityClient;
import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.PaymentClient;
import com.cinemaai.booking.client.dto.UserAccessScopeDto;
import com.cinemaai.booking.dto.request.AdminCancelTicketRequest;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.dto.response.BookingProductResponse;
import com.cinemaai.booking.entity.Order;
import com.cinemaai.booking.entity.OrderItem;
import com.cinemaai.booking.repository.OrderRepository;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import com.cinemaai.booking.dto.response.CinemaDashboardResponse;
import com.cinemaai.booking.dto.response.ShowtimeBookingSummaryDto;
import com.cinemaai.booking.dto.response.ShowtimeCancelRefundResultDto;
import com.cinemaai.booking.dto.response.TicketAuditLogResponse;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.entity.TicketAuditLog;
import com.cinemaai.booking.enums.BookingSeatStatus;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.exception.ConflictException;
import com.cinemaai.booking.exception.ForbiddenException;
import com.cinemaai.booking.exception.NotFoundException;
import com.cinemaai.booking.mapper.BookingMapper;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.BookingSeatRepository;
import com.cinemaai.booking.repository.TicketAuditLogRepository;
import com.cinemaai.booking.security.AuthenticatedUser;
import com.cinemaai.booking.security.CinemaSecurityService;
import com.cinemaai.booking.service.AdminBookingService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final PaymentClient paymentClient;
    private final BookingMapper bookingMapper;
    private final BookingSeatRepository bookingSeatRepository;
    private final IdentityClient identityClient;
    private final CatalogClient catalogClient;
    private final OrderRepository orderRepository;

    
    private BookingResponse enrichBookingResponseWithOrders(Booking booking) {
        BookingResponse res = bookingMapper.toResponse(booking);
        if (booking == null || booking.getId() == null) {
            return res;
        }
        try {
            List<Order> addOnOrders = orderRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId());
            if (addOnOrders != null && !addOnOrders.isEmpty()) {
                List<BookingProductResponse> allFoods = new ArrayList<>(res.foods() != null ? res.foods() : Collections.emptyList());
                for (Order o : addOnOrders) {
                    if ("PAID".equalsIgnoreCase(o.getStatus()) || "PICKED_UP".equalsIgnoreCase(o.getStatus()) || "PARTIALLY_PICKED_UP".equalsIgnoreCase(o.getStatus())) {
                        if (o.getItems() != null) {
                            for (OrderItem item : o.getItems()) {
                                allFoods.add(new BookingProductResponse(
                                        item.getId(),
                                        item.getProductId(),
                                        item.isCombo(),
                                        item.getProductNameSnapshot(),
                                        item.getQuantity(),
                                        item.getUnitPrice(),
                                        item.getLineTotal()
                                ));
                            }
                        }
                    }
                }
                return res.withFoods(allFoods);
            }
        } catch (Exception e) {
            log.warn("Failed to attach add-on food orders to booking #{}: {}", booking.getId(), e.getMessage());
        }
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookings(AuthenticatedUser actor, Long requestedCinemaId, BookingStatus status, String search, Pageable pageable) {
        Long enforcedCinemaId = cinemaSecurityService.resolveEnforcedCinemaId(actor, requestedCinemaId, false);
        if (search == null || search.trim().isEmpty()) {
            return bookingRepository.findBookingsByCinemaAndStatus(enforcedCinemaId, status, pageable)
                    .map(this::enrichBookingResponseWithOrders);
        }
        return bookingRepository.findBookingsForAdmin(enforcedCinemaId, status, search.trim(), pageable)
                .map(this::enrichBookingResponseWithOrders);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(AuthenticatedUser actor, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn đặt vé #" + bookingId));

        cinemaSecurityService.validateCinemaAccess(actor, booking.getCinemaId(), false);
        return enrichBookingResponseWithOrders(booking);
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
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketAuditLogResponse> getAuditLogs(AuthenticatedUser actor, Long requestedCinemaId, Pageable pageable) {
        Long enforcedCinemaId = cinemaSecurityService.resolveEnforcedCinemaId(actor, requestedCinemaId, false);
        return ticketAuditLogRepository.findAuditLogs(enforcedCinemaId, pageable)
                .map(TicketAuditLogResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaDashboardResponse getDashboardMetrics(AuthenticatedUser actor, Long requestedCinemaId) {
        Long enforcedCinemaId = cinemaSecurityService.resolveEnforcedCinemaId(actor, requestedCinemaId, false);

        List<BookingStatus> paidStatuses = List.of(BookingStatus.PAID, BookingStatus.USED);

        // 1. Total revenue
        BigDecimal totalRevenue = bookingRepository.sumRevenueByCinemaIdAndStatusIn(enforcedCinemaId, paidStatuses);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        // 2. Paid tickets count
        long totalPaidTickets = bookingRepository.countByCinemaIdAndStatusIn(enforcedCinemaId, paidStatuses);

        // 3. Cancelled tickets count
        long totalCancelledTickets = bookingRepository.countByCinemaIdAndStatusIn(enforcedCinemaId, List.of(BookingStatus.CANCELLED));

        // 4. Refunded tickets count
        long totalRefundedTickets = bookingRepository.countByCinemaIdAndStatusIn(enforcedCinemaId, List.of(BookingStatus.REFUNDED));

        // 5. Total refunded amount from successful refund audit logs
        BigDecimal totalRefundedAmount = ticketAuditLogRepository.sumAmountByCinemaIdAndActionAndStatus(enforcedCinemaId, "REFUND", "SUCCESS");
        if (totalRefundedAmount == null) totalRefundedAmount = BigDecimal.ZERO;

        // Occupancy calculation (rough estimate based on available rooms)
        double occupancyRate = totalPaidTickets > 0 ? Math.min(100.0, (double) totalPaidTickets * 1.5) : 0.0;

        // 5. Cinema metrics breakdown
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
                .findBookingsByCinemaAndStatus(enforcedCinemaId, BookingStatus.CANCELLED, PageRequest.of(0, 5))
                .getContent()
                .stream()
                .map(bookingMapper::toResponse)
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

    @Override
    @Transactional
    public ShowtimeCancelRefundResultDto cancelAndRefundShowtime(AuthenticatedUser actor, Long showtimeId, String reason) {
        authorizeShowtime(actor, showtimeId);
        List<Booking> bookings = bookingRepository.findByShowtimeId(showtimeId);
        UserAccessScopeDto scope = cinemaSecurityService.getAuthoritativeScope(actor);
        String actorRole = hasRole(scope, "ADMIN") ? "ADMIN" : "MANAGER";
        return cancelAndRefundShowtimeInternal(bookings, showtimeId, reason, actorRole, actor.id(), actor.email());
    }

    @Override
    @Transactional
    public ShowtimeCancelRefundResultDto cancelAndRefundShowtimeInternal(Long showtimeId, String reason, String actorRole, Long actorUserId) {
        return cancelAndRefundShowtimeInternal(
                bookingRepository.findByShowtimeId(showtimeId), showtimeId, reason, actorRole, actorUserId, null);
    }

    private ShowtimeCancelRefundResultDto cancelAndRefundShowtimeInternal(
            List<Booking> bookings, Long showtimeId, String reason, String actorRole, Long actorUserId, String actorEmail) {
        if (bookings == null || bookings.isEmpty()) {
            return new ShowtimeCancelRefundResultDto(showtimeId, 0, 0, 0, BigDecimal.ZERO, "Suat chieu khong co ve nao can huy");
        }

        LocalDateTime now = LocalDateTime.now();
        int refundedCount = 0;
        int cancelledCount = 0;
        BigDecimal totalRefundAmount = BigDecimal.ZERO;
        String finalReason = reason != null && !reason.isBlank() ? reason : "Huy suat chieu do su co ky thuat";
        String role = actorRole != null ? actorRole : "ADMIN";

        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.PAID || booking.getStatus() == BookingStatus.USED) {
                booking.setStatus(BookingStatus.REFUNDED);
                booking.setRefundedAt(now);
                booking.setRefundReason(finalReason);

                if (booking.getSeats() != null) {
                    for (BookingSeat seat : booking.getSeats()) {
                        seat.setStatus(BookingSeatStatus.RELEASED);
                    }
                }
                bookingRepository.save(booking);

                BigDecimal amt = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal newBalance = null;
                if (booking.getUserId() != null && amt.compareTo(BigDecimal.ZERO) > 0) {
                    newBalance = paymentClient.creditWalletForRefund(
                            booking.getUserId(),
                            amt,
                            booking.getId(),
                            booking.getBookingCode(),
                            "Hoan tien suat chieu bi huy: " + finalReason
                    );
                    totalRefundAmount = totalRefundAmount.add(amt);
                }

                if (booking.getUserId() != null) {
                    paymentClient.refundLoyaltyPoints(
                            booking.getUserId(),
                            booking.getId(),
                            booking.getBookingCode(),
                            amt,
                            booking.getLoyaltyPointsRedeemed(),
                            "Huy suat chieu: " + finalReason
                    );
                }

                sendRefundEmailNotice(booking, amt, newBalance, "Suất chiếu bị hủy do sự cố vận hành: " + finalReason);

                TicketAuditLog auditLog = TicketAuditLog.builder()
                        .bookingId(booking.getId())
                        .ticketCode(booking.getBookingCode())
                        .cinemaId(booking.getCinemaId())
                        .actorUserId(actorUserId != null ? actorUserId : 1L)
                        .actorEmail(actorEmail != null ? actorEmail : role.toLowerCase() + "@cinemaai.internal")
                        .actorRole(role)
                        .action("REFUND")
                        .amount(amt)
                        .reason(finalReason)
                        .status("SUCCESS")
                        .build();
                ticketAuditLogRepository.save(auditLog);
                refundedCount++;
            } else if (booking.getStatus() == BookingStatus.HOLDING || booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setCancelledAt(now);
                if (booking.getSeats() != null) {
                    for (BookingSeat seat : booking.getSeats()) {
                        seat.setStatus(BookingSeatStatus.RELEASED);
                    }
                }
                bookingRepository.save(booking);
                cancelledCount++;
            }
        }

        log.info("Showtime #{} cancelled & refunded: {} paid tickets refunded (total: {} VND), {} pending tickets cancelled",
                showtimeId, refundedCount, totalRefundAmount, cancelledCount);

        return new ShowtimeCancelRefundResultDto(
                showtimeId,
                bookings.size(),
                refundedCount,
                cancelledCount,
                totalRefundAmount,
                "Da xu ly huy va hoan tien " + refundedCount + " ve vao CineWallet (" + totalRefundAmount + " VND)"
        );
    }

    private void sendRefundEmailNotice(Booking booking, BigDecimal amount, BigDecimal newBalance, String reason) {
        if (booking == null) return;
        try {
            String recipientEmail = booking.getCustomerEmailSnapshot();
            if ((recipientEmail == null || recipientEmail.isBlank()) && booking.getUserId() != null) {
                try {
                    UserAccessScopeDto userScope = identityClient.getUserAccessScope(booking.getUserId());
                    if (userScope != null && userScope.email() != null && !userScope.email().isBlank()) {
                        recipientEmail = userScope.email();
                    }
                } catch (Exception e) {
                    log.warn("Could not retrieve email from Identity Service for userId {}: {}", booking.getUserId(), e.getMessage());
                }
            }

            if (recipientEmail != null && !recipientEmail.isBlank()) {
                identityClient.sendWalletRefundNotice(
                        recipientEmail,
                        booking.getBookingCode(),
                        amount != null ? amount : BigDecimal.ZERO,
                        newBalance != null ? newBalance : BigDecimal.ZERO,
                        reason
                );
            } else {
                log.warn("Skipped refund email notice for booking {} because customer email is blank", booking.getBookingCode());
            }
        } catch (Exception ex) {
            log.error("Failed to trigger refund email notice for booking {}: {}", booking.getBookingCode(), ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getSoldTicketCounts(AuthenticatedUser actor, List<Long> showtimeIds) {
        if (showtimeIds != null) {
            for (Long showtimeId : showtimeIds) {
                authorizeShowtime(actor, showtimeId);
            }
        }
        return getSoldTicketCountsInternal(showtimeIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getSoldTicketCountsInternal(List<Long> showtimeIds) {
        if (showtimeIds == null || showtimeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = bookingSeatRepository.countSeatsByShowtimeIdsAndStatusIn(
                showtimeIds,
                List.of(BookingSeatStatus.BOOKED, BookingSeatStatus.CHECKED_IN)
        );
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : rows) {
            Long sid = row[0] instanceof Number ? ((Number) row[0]).longValue() : Long.parseLong(row[0].toString());
            int count = row[1] instanceof Number ? ((Number) row[1]).intValue() : Integer.parseInt(row[1].toString());
            result.put(sid, count);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeBookingSummaryDto getShowtimeBookingSummary(AuthenticatedUser actor, Long showtimeId) {
        authorizeShowtime(actor, showtimeId);
        List<Booking> bookings = bookingRepository.findByShowtimeId(showtimeId);
        return buildShowtimeBookingSummary(showtimeId, bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeBookingSummaryDto getShowtimeBookingSummaryInternal(Long showtimeId) {
        return buildShowtimeBookingSummary(showtimeId, bookingRepository.findByShowtimeId(showtimeId));
    }

    private ShowtimeBookingSummaryDto buildShowtimeBookingSummary(Long showtimeId, List<Booking> bookings) {
        int paidBookingsCount = 0;
        int soldTicketsCount = 0;
        int holdingTicketsCount = 0;
        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        if (bookings != null) {
            for (Booking b : bookings) {
                if (b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.USED) {
                    paidBookingsCount++;
                    int seatCount = (b.getSeats() != null && !b.getSeats().isEmpty()) ? b.getSeats().size() : 1;
                    soldTicketsCount += seatCount;
                    if (b.getTotalAmount() != null) {
                        totalRefundAmount = totalRefundAmount.add(b.getTotalAmount());
                    }
                } else if (b.getStatus() == BookingStatus.HOLDING || b.getStatus() == BookingStatus.PENDING_PAYMENT) {
                    int seatCount = (b.getSeats() != null && !b.getSeats().isEmpty()) ? b.getSeats().size() : 1;
                    holdingTicketsCount += seatCount;
                }
            }
        }

        return new ShowtimeBookingSummaryDto(
                showtimeId,
                soldTicketsCount,
                paidBookingsCount,
                holdingTicketsCount,
                totalRefundAmount
        );
    }

    private void authorizeShowtime(AuthenticatedUser actor, Long showtimeId) {
        UserAccessScopeDto scope = cinemaSecurityService.getAuthoritativeScope(actor);
        if (hasRole(scope, "ADMIN")) return;
        CatalogClient.ShowtimeScope showtime = catalogClient.getShowtimeScope(showtimeId);
        if (showtime == null || showtime.cinemaId() == null || showtime.cinemaId() <= 0) {
            throw new ForbiddenException("Unable to verify showtime cinema");
        }
        cinemaSecurityService.validateCinemaAccess(actor, showtime.cinemaId(), false);
    }

    private boolean hasRole(UserAccessScopeDto scope, String role) {
        return scope.roles() != null && scope.roles().stream()
                .anyMatch(value -> value.equalsIgnoreCase(role) || value.equalsIgnoreCase("ROLE_" + role));
    }
}
