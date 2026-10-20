package com.cinemaai.booking.service;

import com.cinemaai.booking.dto.request.AdminCancelTicketRequest;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.dto.response.ShowtimeBookingSummaryDto;
import java.util.Map;
import java.util.List;
import com.cinemaai.booking.dto.response.ShowtimeCancelRefundResultDto;
import com.cinemaai.booking.dto.response.CinemaDashboardResponse;
import com.cinemaai.booking.dto.response.TicketAuditLogResponse;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminBookingService {

    Page<BookingResponse> getBookings(AuthenticatedUser actor, Long requestedCinemaId, BookingStatus status, String search, Pageable pageable);

    BookingResponse getBookingById(AuthenticatedUser actor, Long bookingId);

    BookingResponse cancelBooking(AuthenticatedUser actor, Long bookingId, AdminCancelTicketRequest request);

    Page<TicketAuditLogResponse> getAuditLogs(AuthenticatedUser actor, Long requestedCinemaId, Pageable pageable);

    CinemaDashboardResponse getDashboardMetrics(AuthenticatedUser actor, Long requestedCinemaId);

    ShowtimeCancelRefundResultDto cancelAndRefundShowtime(Long showtimeId, String reason, String actorRole, Long actorUserId);

    Map<Long, Integer> getSoldTicketCounts(List<Long> showtimeIds);

    ShowtimeBookingSummaryDto getShowtimeBookingSummary(Long showtimeId);
}
