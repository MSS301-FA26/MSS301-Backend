package com.cinemaai.booking.service;

import com.cinemaai.booking.client.dto.UserAccessScopeDto;
import com.cinemaai.booking.dto.request.AdminCancelTicketRequest;
import com.cinemaai.booking.dto.request.AdminRefundTicketRequest;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.dto.response.CinemaDashboardResponse;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.entity.TicketAuditLog;
import com.cinemaai.booking.enums.BookingSeatStatus;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.exception.ConflictException;
import com.cinemaai.booking.exception.ForbiddenException;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.TicketAuditLogRepository;
import com.cinemaai.booking.security.AuthenticatedUser;
import com.cinemaai.booking.security.CinemaSecurityService;
import com.cinemaai.booking.client.IdentityClient;
import com.cinemaai.booking.client.PaymentClient;
import com.cinemaai.booking.service.impl.AdminBookingServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TicketAuditLogRepository ticketAuditLogRepository;
    @Mock
    private CinemaSecurityService cinemaSecurityService;
    @Mock
    private PaymentClient paymentClient;
    @Mock
    private com.cinemaai.booking.repository.BookingSeatRepository bookingSeatRepository;
    @Mock
    private IdentityClient identityClient;

    private AdminBookingServiceImpl adminBookingService;

    @BeforeEach
    void setUp() {
        adminBookingService = new AdminBookingServiceImpl(
                bookingRepository,
                ticketAuditLogRepository,
                cinemaSecurityService,
                paymentClient,
                bookingSeatRepository,
                identityClient
        );
    }

    @Test
    void testCancelBooking_ManagerSameCinemaSuccess() {
        Long managerId = 10L;
        Long cinemaAId = 101L;
        Long bookingId = 500L;

        AuthenticatedUser manager = new AuthenticatedUser(managerId, "mgr@test.com", List.of(new SimpleGrantedAuthority("ROLE_MANAGER")), cinemaAId);

        BookingSeat seat = BookingSeat.builder().seatNumber(1).rowLabel("A").status(BookingSeatStatus.BOOKED).build();
        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingCode("BK123456")
                .cinemaId(cinemaAId)
                .userId(99L)
                .showtimeId(1L)
                .movieId(1L)
                .cinemaNameSnapshot("Cinema A")
                .roomNameSnapshot("Room 1")
                .showtimeStartSnapshot(LocalDateTime.now().plusHours(2))
                .status(BookingStatus.PAID)
                .totalAmount(new BigDecimal("150000.00"))
                .seats(new ArrayList<>(List.of(seat)))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(cinemaSecurityService.getAuthoritativeScope(manager)).thenReturn(
                new UserAccessScopeDto(managerId, "mgr@test.com", "ACTIVE", List.of("MANAGER"), cinemaAId)
        );

        AdminCancelTicketRequest req = new AdminCancelTicketRequest("Khách muốn hủy vé");
        BookingResponse resp = adminBookingService.cancelBooking(manager, bookingId, req);

        assertNotNull(resp);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(BookingSeatStatus.RELEASED, seat.getStatus());

        ArgumentCaptor<TicketAuditLog> auditCaptor = ArgumentCaptor.forClass(TicketAuditLog.class);
        verify(ticketAuditLogRepository).save(auditCaptor.capture());
        TicketAuditLog savedLog = auditCaptor.getValue();

        assertEquals("CANCEL", savedLog.getAction());
        assertEquals("SUCCESS", savedLog.getStatus());
        assertEquals(cinemaAId, savedLog.getCinemaId());
        assertEquals(managerId, savedLog.getActorUserId());
    }

    @Test
    void testCancelBooking_ManagerDifferentCinemaBlocked() {
        Long managerId = 10L;
        Long cinemaAId = 101L;
        Long cinemaBId = 202L;
        Long bookingId = 501L;

        AuthenticatedUser manager = new AuthenticatedUser(managerId, "mgr@test.com", List.of(new SimpleGrantedAuthority("ROLE_MANAGER")), cinemaAId);

        Booking booking = Booking.builder()
                .id(bookingId)
                .cinemaId(cinemaBId) // Booking belongs to Cinema B
                .status(BookingStatus.PAID)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        doThrow(new ForbiddenException("Bạn không có quyền thao tác trên dữ liệu của rạp khác"))
                .when(cinemaSecurityService).validateCinemaAccess(manager, cinemaBId, false);

        AdminCancelTicketRequest req = new AdminCancelTicketRequest("Hủy vé");
        assertThrows(ForbiddenException.class, () -> adminBookingService.cancelBooking(manager, bookingId, req));

        verify(bookingRepository, never()).save(any(Booking.class));
        verify(ticketAuditLogRepository, never()).save(any(TicketAuditLog.class));
    }

    @Test
    void testCancelBooking_AlreadyUsedTicketBlocked() {
        Long managerId = 10L;
        Long cinemaAId = 101L;
        Long bookingId = 502L;

        AuthenticatedUser manager = new AuthenticatedUser(managerId, "mgr@test.com", List.of(new SimpleGrantedAuthority("ROLE_MANAGER")), cinemaAId);

        Booking booking = Booking.builder()
                .id(bookingId)
                .cinemaId(cinemaAId)
                .status(BookingStatus.USED) // Already checked-in
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        AdminCancelTicketRequest req = new AdminCancelTicketRequest("Hủy vé");
        assertThrows(ConflictException.class, () -> adminBookingService.cancelBooking(manager, bookingId, req));
    }

    @Test
    void testRefundBooking_Success() {
        Long managerId = 10L;
        Long cinemaAId = 101L;
        Long bookingId = 503L;

        AuthenticatedUser manager = new AuthenticatedUser(managerId, "mgr@test.com", List.of(new SimpleGrantedAuthority("ROLE_MANAGER")), cinemaAId);

        BookingSeat seat = BookingSeat.builder().seatNumber(1).rowLabel("B").status(BookingSeatStatus.BOOKED).build();
        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingCode("BK789012")
                .cinemaId(cinemaAId)
                .userId(99L)
                .showtimeId(1L)
                .movieId(1L)
                .cinemaNameSnapshot("Cinema A")
                .roomNameSnapshot("Room 1")
                .showtimeStartSnapshot(LocalDateTime.now().plusHours(2))
                .status(BookingStatus.PAID)
                .totalAmount(new BigDecimal("100000.00"))
                .seats(new ArrayList<>(List.of(seat)))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(cinemaSecurityService.getAuthoritativeScope(manager)).thenReturn(
                new UserAccessScopeDto(managerId, "mgr@test.com", "ACTIVE", List.of("MANAGER"), cinemaAId)
        );

        AdminRefundTicketRequest req = new AdminRefundTicketRequest("Hoàn tiền qua cổng VNPay");
        BookingResponse resp = adminBookingService.refundBooking(manager, bookingId, req);

        assertNotNull(resp);
        assertEquals(BookingStatus.REFUNDED, booking.getStatus());
        assertNotNull(booking.getRefundedAt());
        assertEquals("Hoàn tiền qua cổng VNPay", booking.getRefundReason());
        assertEquals(BookingSeatStatus.RELEASED, seat.getStatus());

        ArgumentCaptor<TicketAuditLog> auditCaptor = ArgumentCaptor.forClass(TicketAuditLog.class);
        verify(ticketAuditLogRepository).save(auditCaptor.capture());
        TicketAuditLog savedLog = auditCaptor.getValue();

        assertEquals("REFUND", savedLog.getAction());
        assertEquals("SUCCESS", savedLog.getStatus());
        assertEquals(new BigDecimal("100000.00"), savedLog.getAmount());
    }

    @Test
    void testRefundBooking_DoubleRefundBlocked() {
        Long managerId = 10L;
        Long cinemaAId = 101L;
        Long bookingId = 504L;

        AuthenticatedUser manager = new AuthenticatedUser(managerId, "mgr@test.com", List.of(new SimpleGrantedAuthority("ROLE_MANAGER")), cinemaAId);

        Booking booking = Booking.builder()
                .id(bookingId)
                .cinemaId(cinemaAId)
                .status(BookingStatus.REFUNDED) // Already refunded
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        AdminRefundTicketRequest req = new AdminRefundTicketRequest("Thử hoàn lại lần 2");
        assertThrows(ConflictException.class, () -> adminBookingService.refundBooking(manager, bookingId, req));
    }

    @Test
    void testRefundBooking_UnpaidTicketBlocked() {
        Long managerId = 10L;
        Long cinemaAId = 101L;
        Long bookingId = 505L;

        AuthenticatedUser manager = new AuthenticatedUser(managerId, "mgr@test.com", List.of(new SimpleGrantedAuthority("ROLE_MANAGER")), cinemaAId);

        Booking booking = Booking.builder()
                .id(bookingId)
                .cinemaId(cinemaAId)
                .status(BookingStatus.HOLDING) // Unpaid
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        AdminRefundTicketRequest req = new AdminRefundTicketRequest("Hoàn tiền cho vé chưa trả tiền");
        assertThrows(BadRequestException.class, () -> adminBookingService.refundBooking(manager, bookingId, req));
    }

    @Test
    void testDashboardMetrics_StrictlyScopesManagerToAssignedCinema() {
        Long managerId = 10L;
        Long cinemaAId = 101L;

        AuthenticatedUser manager = new AuthenticatedUser(managerId, "mgr@test.com", List.of(new SimpleGrantedAuthority("ROLE_MANAGER")), cinemaAId);

        when(cinemaSecurityService.resolveEnforcedCinemaId(manager, null, false)).thenReturn(cinemaAId);
        when(bookingRepository.sumRevenueByCinemaIdAndStatusIn(cinemaAId, List.of(BookingStatus.PAID, BookingStatus.USED)))
                .thenReturn(new BigDecimal("5000000.00"));
        when(bookingRepository.countByCinemaIdAndStatusIn(cinemaAId, List.of(BookingStatus.PAID, BookingStatus.USED)))
                .thenReturn(50L);
        when(bookingRepository.countByCinemaIdAndStatusIn(cinemaAId, List.of(BookingStatus.CANCELLED)))
                .thenReturn(2L);
        when(bookingRepository.countByCinemaIdAndStatusIn(cinemaAId, List.of(BookingStatus.REFUNDED)))
                .thenReturn(1L);
        when(ticketAuditLogRepository.sumAmountByCinemaIdAndActionAndStatus(cinemaAId, "REFUND", "SUCCESS"))
                .thenReturn(new BigDecimal("100000.00"));
        when(ticketAuditLogRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(bookingRepository.findBookingsByCinemaAndStatus(eq(cinemaAId), eq(BookingStatus.CANCELLED), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        CinemaDashboardResponse metrics = adminBookingService.getDashboardMetrics(manager, null);

        assertNotNull(metrics);
        assertEquals(cinemaAId, metrics.cinemaId());
        assertEquals(new BigDecimal("5000000.00"), metrics.totalRevenue());
        assertEquals(50L, metrics.totalPaidTickets());
        assertEquals(2L, metrics.totalCancelledTickets());
        assertEquals(1L, metrics.totalRefundedTickets());
        assertEquals(new BigDecimal("100000.00"), metrics.totalRefundedAmount());
    }

    @Test
    void testRefundBooking_SendsWalletRefundEmailNotice() {
        Long managerId = 10L;
        Long cinemaAId = 101L;
        Long bookingId = 506L;

        AuthenticatedUser manager = new AuthenticatedUser(managerId, "mgr@test.com", List.of(new SimpleGrantedAuthority("ROLE_MANAGER")), cinemaAId);

        BookingSeat seat = BookingSeat.builder().seatNumber(1).rowLabel("B").status(BookingSeatStatus.BOOKED).build();
        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingCode("BK_REFUND_MAIL")
                .cinemaId(cinemaAId)
                .userId(99L)
                .customerEmailSnapshot("customer@example.com")
                .showtimeId(1L)
                .movieId(1L)
                .cinemaNameSnapshot("Cinema A")
                .roomNameSnapshot("Room 1")
                .showtimeStartSnapshot(LocalDateTime.now().plusHours(2))
                .status(BookingStatus.PAID)
                .totalAmount(new BigDecimal("150000.00"))
                .seats(new ArrayList<>(List.of(seat)))
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(cinemaSecurityService.getAuthoritativeScope(manager)).thenReturn(
                new UserAccessScopeDto(managerId, "mgr@test.com", "ACTIVE", List.of("MANAGER"), cinemaAId)
        );
        when(paymentClient.creditWalletForRefund(eq(99L), eq(new BigDecimal("150000.00")), eq(bookingId), eq("BK_REFUND_MAIL"), any()))
                .thenReturn(new BigDecimal("200000.00"));

        AdminRefundTicketRequest req = new AdminRefundTicketRequest("Đổi lịch chiếu cá nhân");
        BookingResponse resp = adminBookingService.refundBooking(manager, bookingId, req);

        assertNotNull(resp);
        assertEquals(BookingStatus.REFUNDED, booking.getStatus());
        verify(identityClient).sendWalletRefundNotice(
                eq("customer@example.com"),
                eq("BK_REFUND_MAIL"),
                eq(new BigDecimal("150000.00")),
                eq(new BigDecimal("200000.00")),
                eq("Đổi lịch chiếu cá nhân")
        );
    }
}
