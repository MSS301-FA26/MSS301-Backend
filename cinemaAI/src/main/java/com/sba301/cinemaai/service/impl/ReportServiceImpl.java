package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.response.report.AbandonedRateResponse;
import com.sba301.cinemaai.dto.response.report.ConcessionSalesResponse;
import com.sba301.cinemaai.dto.response.report.ExpiredUserResponse;
import com.sba301.cinemaai.dto.response.report.NoShowReportResponse;
import com.sba301.cinemaai.dto.response.report.PeakHourResponse;
import com.sba301.cinemaai.dto.response.report.RevenueReportResponse;
import com.sba301.cinemaai.dto.response.report.RoomOccupancyResponse;
import com.sba301.cinemaai.dto.response.report.ShowtimeFillResponse;
import com.sba301.cinemaai.dto.response.report.TopMovieResponse;
import com.sba301.cinemaai.dto.response.report.TopSeatResponse;
import com.sba301.cinemaai.dto.response.report.IncidentRefundedUser;
import com.sba301.cinemaai.dto.response.report.ShowtimeIncidentItem;
import com.sba301.cinemaai.dto.response.report.ShowtimeIncidentReportResponse;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.BookingStatus;
import com.sba301.cinemaai.enums.ShowtimeStatus;
import com.sba301.cinemaai.repository.BookingFoodItemRepository;
import com.sba301.cinemaai.repository.BookingRepository;
import com.sba301.cinemaai.repository.BookingSeatRepository;
import com.sba301.cinemaai.repository.PaymentRepository;
import com.sba301.cinemaai.repository.ShowtimeRepository;
import com.sba301.cinemaai.service.ReportService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingFoodItemRepository bookingFoodItemRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingSeatRepository bookingSeatRepository;

    @Transactional(readOnly = true)
    @Override
    public RevenueReportResponse getRevenue(LocalDate from, LocalDate to) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate safeTo = to != null ? to : LocalDate.now();
        LocalDateTime dtFrom = safeFrom.atStartOfDay();
        LocalDateTime dtTo = safeTo.plusDays(1).atStartOfDay();

        BigDecimal revenue = paymentRepository.sumRevenue(dtFrom, dtTo);
        long transactions = paymentRepository.countSuccessfulPayments(dtFrom, dtTo);
        long tickets = bookingRepository.countTicketsSold(dtFrom, dtTo);

        List<com.sba301.cinemaai.dto.response.report.ProviderRevenueResponse> byProvider =
                paymentRepository.revenueByProvider(dtFrom, dtTo)
                        .stream()
                        .map(row -> new com.sba301.cinemaai.dto.response.report.ProviderRevenueResponse(
                                String.valueOf(row[0]),
                                row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO,
                                ((Number) row[2]).longValue()
                        ))
                        .toList();

        return new RevenueReportResponse(safeFrom, safeTo, revenue, transactions, tickets, byProvider);
    }

    @Transactional(readOnly = true)
    @Override
    public List<TopMovieResponse> getTopMovies(LocalDate from, LocalDate to, int limit) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate safeTo = to != null ? to : LocalDate.now();
        LocalDateTime dtFrom = safeFrom.atStartOfDay();
        LocalDateTime dtTo = safeTo.plusDays(1).atStartOfDay();

        int safeLimit = Math.max(1, Math.min(limit, 50));
        return bookingRepository.topMoviesByTickets(dtFrom, dtTo)
                .stream()
                .limit(safeLimit)
                .map(row -> new TopMovieResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<RoomOccupancyResponse> getRoomOccupancy(LocalDate from, LocalDate to) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate safeTo = to != null ? to : LocalDate.now();
        LocalDateTime dtFrom = safeFrom.atStartOfDay();
        LocalDateTime dtTo = safeTo.plusDays(1).atStartOfDay();

        Map<Long, Long> soldByRoom = new java.util.HashMap<>();
        bookingRepository.soldSeatsByRoom(dtFrom, dtTo)
                .forEach(row -> soldByRoom.put(((Number) row[0]).longValue(), ((Number) row[2]).longValue()));

        // Mẫu số = sức chứa phòng × số suất trong khoảng — phòng có suất mà 0 vé vẫn hiện
        return bookingRepository.roomShowCapacity(dtFrom, dtTo)
                .stream()
                .map(row -> {
                    long roomId = ((Number) row[0]).longValue();
                    long showtimes = ((Number) row[2]).longValue();
                    long seatsPerShow = ((Number) row[3]).longValue();
                    long capacityTotal = showtimes * seatsPerShow;
                    long sold = soldByRoom.getOrDefault(roomId, 0L);
                    double rate = capacityTotal > 0 ? (double) sold / capacityTotal * 100.0 : 0.0;
                    return new RoomOccupancyResponse(
                            roomId,
                            (String) row[1],
                            sold,
                            capacityTotal,
                            Math.min(rate, 100.0),
                            showtimes
                    );
                })
                .toList();
    }

    /**
     * Tỷ lệ lấp đầy theo TỪNG NGÀY (mọi phòng gộp lại): tái dùng query per-showtime
     * `showtimeFill` rồi gộp theo ngày của suất chiếu.
     */
    @Transactional(readOnly = true)
    @Override
    public List<com.sba301.cinemaai.dto.response.report.DailyOccupancyResponse> getDailyOccupancy(LocalDate from, LocalDate to) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate safeTo = to != null ? to : LocalDate.now();

        Map<LocalDate, long[]> byDay = new java.util.TreeMap<>();
        bookingRepository.showtimeFill(safeFrom.atStartOfDay(), safeTo.plusDays(1).atStartOfDay())
                .forEach(row -> {
                    LocalDate day = ((LocalDateTime) row[3]).toLocalDate();
                    long capacity = ((Number) row[4]).longValue();
                    long sold = ((Number) row[5]).longValue();
                    long[] acc = byDay.computeIfAbsent(day, k -> new long[3]);
                    acc[0] += sold;
                    acc[1] += capacity;
                    acc[2] += 1;
                });

        return byDay.entrySet().stream()
                .map(entry -> {
                    long sold = entry.getValue()[0];
                    long capacity = entry.getValue()[1];
                    double rate = capacity > 0 ? (double) sold / capacity * 100.0 : 0.0;
                    return new com.sba301.cinemaai.dto.response.report.DailyOccupancyResponse(
                            entry.getKey(), sold, capacity, entry.getValue()[2], Math.min(rate, 100.0));
                })
                .toList();
    }

    /** Ghế đã bán / còn trống theo từng suất chiếu, mặc định nhìn tới trước `days` ngày kể từ `date`. */
    @Transactional(readOnly = true)
    @Override
    public List<ShowtimeFillResponse> getShowtimeFill(LocalDate date, int days) {
        LocalDate safeDate = date != null ? date : LocalDate.now();
        int safeDays = Math.max(1, Math.min(days, 30));
        LocalDateTime from = safeDate.atStartOfDay();
        LocalDateTime to = safeDate.plusDays(safeDays).atStartOfDay();

        return bookingRepository.showtimeFill(from, to)
                .stream()
                .map(row -> {
                    long capacity = ((Number) row[4]).longValue();
                    long sold = ((Number) row[5]).longValue();
                    return new ShowtimeFillResponse(
                            ((Number) row[0]).longValue(),
                            (String) row[1],
                            (String) row[2],
                            (LocalDateTime) row[3],
                            capacity,
                            sold,
                            Math.max(0, capacity - sold)
                    );
                })
                .toList();
    }

    /** Người mua vé (PAID) nhưng không đến check-in dù suất chiếu đã kết thúc. */
    @Transactional(readOnly = true)
    @Override
    public NoShowReportResponse getNoShows(LocalDate from, LocalDate to) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate safeTo = to != null ? to : LocalDate.now();
        LocalDateTime dtFrom = safeFrom.atStartOfDay();
        LocalDateTime dtTo = safeTo.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        long noShows = bookingRepository.countNoShows(dtFrom, dtTo, now);
        long finished = bookingRepository.countFinishedPaidBookings(dtFrom, dtTo, now);
        double rate = finished > 0 ? (double) noShows / finished * 100.0 : 0.0;
        return new NoShowReportResponse(safeFrom, safeTo, noShows, finished, rate);
    }

    /** Histogram vé bán theo giờ bắt đầu suất chiếu — tìm giờ cao điểm để điều chỉnh giá. */
    @Transactional(readOnly = true)
    @Override
    public List<PeakHourResponse> getPeakHours(LocalDate from, LocalDate to) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate safeTo = to != null ? to : LocalDate.now();
        return bookingRepository.ticketsByHourOfDay(safeFrom.atStartOfDay(), safeTo.plusDays(1).atStartOfDay())
                .stream()
                .map(row -> new PeakHourResponse(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    /** Xếp hạng ghế theo số lần được mua — phân tích vị trí khách ưa chuộng; roomId null = mọi phòng. */
    @Transactional(readOnly = true)
    @Override
    public List<TopSeatResponse> getTopSeats(LocalDate from, LocalDate to, Long roomId) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate safeTo = to != null ? to : LocalDate.now();
        return bookingRepository.topSeats(safeFrom.atStartOfDay(), safeTo.plusDays(1).atStartOfDay(), roomId)
                .stream()
                .map(row -> new TopSeatResponse(
                        (String) row[0],
                        ((Number) row[1]).intValue(),
                        String.valueOf(row[2]),
                        ((Number) row[3]).longValue()
                ))
                .toList();
    }

    /** Tỷ lệ đặt vé rồi bỏ ngang không thanh toán (EXPIRED) so với số đơn đã trả tiền. */
    @Transactional(readOnly = true)
    @Override
    public AbandonedRateResponse getAbandonedRate(LocalDate from, LocalDate to) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate safeTo = to != null ? to : LocalDate.now();
        Map<BookingStatus, Long> counts = new EnumMap<>(BookingStatus.class);
        bookingRepository.countByStatusInRange(safeFrom.atStartOfDay(), safeTo.plusDays(1).atStartOfDay())
                .forEach(row -> counts.put((BookingStatus) row[0], ((Number) row[1]).longValue()));

        long expired = counts.getOrDefault(BookingStatus.EXPIRED, 0L);
        long cancelled = counts.getOrDefault(BookingStatus.CANCELLED, 0L);
        long paid = counts.getOrDefault(BookingStatus.PAID, 0L) + counts.getOrDefault(BookingStatus.USED, 0L);
        long attempted = expired + paid;
        double rate = attempted > 0 ? (double) expired / attempted * 100.0 : 0.0;
        return new AbandonedRateResponse(safeFrom, safeTo, expired, cancelled, paid, rate);
    }

    /** Doanh số bắp nước (gồm cả đơn đặt thêm sau thanh toán đã PAID). */
    @Transactional(readOnly = true)
    @Override
    public ConcessionSalesResponse getConcessionSales(LocalDate from, LocalDate to) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate safeTo = to != null ? to : LocalDate.now();
        if (safeFrom.isAfter(safeTo)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        if (ChronoUnit.DAYS.between(safeFrom, safeTo) > 366) {
            throw new IllegalArgumentException("Khoảng báo cáo F&B không được vượt quá 366 ngày");
        }

        LocalDateTime rangeFrom = safeFrom.atStartOfDay();
        LocalDateTime rangeTo = safeTo.plusDays(1).atStartOfDay();
        List<ConcessionSalesResponse.Line> lines = bookingFoodItemRepository
                .concessionSales(rangeFrom, rangeTo)
                .stream()
                .map(row -> new ConcessionSalesResponse.Line(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO
                ))
                .toList();
        long totalItems = lines.stream().mapToLong(ConcessionSalesResponse.Line::quantity).sum();
        BigDecimal totalRevenue = lines.stream()
                .map(ConcessionSalesResponse.Line::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<LocalDate, SalesAccumulator> daily = new LinkedHashMap<>();
        for (LocalDate date = safeFrom; !date.isAfter(safeTo); date = date.plusDays(1)) {
            daily.put(date, new SalesAccumulator());
        }

        Map<String, SalesAccumulator> sources = new LinkedHashMap<>();
        sources.put("WITH_TICKET", new SalesAccumulator());
        sources.put("ADD_ON", new SalesAccumulator());
        sources.put("STANDALONE", new SalesAccumulator());

        List<Object[]> transactions = bookingFoodItemRepository.concessionTransactions(rangeFrom, rangeTo);
        transactions.forEach(row -> {
            LocalDate date = ((LocalDateTime) row[0]).toLocalDate();
            String source = String.valueOf(row[1]);
            long quantity = ((Number) row[3]).longValue();
            BigDecimal revenue = row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO;
            daily.computeIfAbsent(date, ignored -> new SalesAccumulator()).addOrder(quantity, revenue);
            sources.computeIfAbsent(source, ignored -> new SalesAccumulator()).addOrder(quantity, revenue);
        });

        long totalOrders = transactions.size();
        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<ConcessionSalesResponse.DailyLine> dailyLines = daily.entrySet().stream()
                .map(entry -> new ConcessionSalesResponse.DailyLine(
                        entry.getKey(),
                        entry.getValue().orderCount,
                        entry.getValue().quantity,
                        entry.getValue().revenue
                ))
                .toList();
        List<ConcessionSalesResponse.SourceLine> sourceLines = sources.entrySet().stream()
                .map(entry -> new ConcessionSalesResponse.SourceLine(
                        entry.getKey(),
                        entry.getValue().orderCount,
                        entry.getValue().quantity,
                        entry.getValue().revenue
                ))
                .toList();

        return new ConcessionSalesResponse(
                safeFrom,
                safeTo,
                totalOrders,
                totalItems,
                totalRevenue,
                averageOrderValue,
                dailyLines,
                lines,
                sourceLines
        );
    }

    private static final class SalesAccumulator {
        private long orderCount;
        private long quantity;
        private BigDecimal revenue = BigDecimal.ZERO;

        private void addOrder(long itemQuantity, BigDecimal orderRevenue) {
            orderCount++;
            quantity += itemQuantity;
            revenue = revenue.add(orderRevenue);
        }
    }

    /** User đặt vé nhưng hết hạn không thanh toán — để admin cân nhắc tặng điểm/khuyến mãi. */
    @Transactional(readOnly = true)
    @Override
    public List<ExpiredUserResponse> getExpiredUsers(LocalDate from, LocalDate to) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate safeTo = to != null ? to : LocalDate.now();
        return bookingRepository.expiredBookingsByUser(safeFrom.atStartOfDay(), safeTo.plusDays(1).atStartOfDay())
                .stream()
                .limit(50)
                .map(row -> new ExpiredUserResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public ShowtimeIncidentReportResponse getShowtimeIncidents(LocalDate from, LocalDate to, Long showtimeId) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().minusDays(90);
        LocalDate safeTo = to != null ? to : LocalDate.now();
        LocalDateTime dtFrom = safeFrom.atStartOfDay();
        LocalDateTime dtTo = safeTo.plusDays(1).atStartOfDay();

        List<Showtime> cancelledShowtimes;
        if (showtimeId != null) {
            cancelledShowtimes = showtimeRepository.findById(showtimeId).stream()
                    .filter(s -> s.getStatus() == ShowtimeStatus.CANCELLED)
                    .toList();
        } else {
            cancelledShowtimes = showtimeRepository.findByStatus(ShowtimeStatus.CANCELLED).stream()
                    .filter(s -> (s.getCancelledAt() != null && !s.getCancelledAt().isBefore(dtFrom) && s.getCancelledAt().isBefore(dtTo))
                            || (s.getStartTime() != null && !s.getStartTime().isBefore(dtFrom) && s.getStartTime().isBefore(dtTo)))
                    .sorted((a, b) -> {
                        LocalDateTime ta = a.getCancelledAt() != null ? a.getCancelledAt() : a.getStartTime();
                        LocalDateTime tb = b.getCancelledAt() != null ? b.getCancelledAt() : b.getStartTime();
                        return tb.compareTo(ta);
                    })
                    .toList();
        }

        List<ShowtimeIncidentItem> incidentItems = new java.util.ArrayList<>();
        long totalRefundedBookings = 0;
        BigDecimal totalRefundedAmount = BigDecimal.ZERO;
        java.util.Set<Long> uniqueUserIds = new java.util.HashSet<>();

        for (Showtime showtime : cancelledShowtimes) {
            List<Booking> refundedBookings = bookingRepository.findByShowtime(showtime).stream()
                    .filter(b -> b.getStatus() == BookingStatus.REFUNDED)
                    .toList();

            List<IncidentRefundedUser> refundedUsers = new java.util.ArrayList<>();
            BigDecimal showtimeRefundTotal = BigDecimal.ZERO;

            for (Booking booking : refundedBookings) {
                User user = booking.getUser();
                if (user != null) {
                    uniqueUserIds.add(user.getId());
                }

                String seatLabels = bookingSeatRepository.findByBooking(booking).stream()
                        .map(bs -> bs.getSeat() != null ? (bs.getSeat().getRowLabel() + bs.getSeat().getSeatNumber()) : "")
                        .filter(s -> !s.isBlank())
                        .collect(java.util.stream.Collectors.joining(", "));

                BigDecimal amount = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
                showtimeRefundTotal = showtimeRefundTotal.add(amount);

                refundedUsers.add(new IncidentRefundedUser(
                        booking.getId(),
                        booking.getBookingCode(),
                        user != null ? user.getId() : null,
                        user != null && user.getProfile() != null && user.getProfile().getFullName() != null
                                ? user.getProfile().getFullName()
                                : (user != null ? user.getEmail() : "Khách hàng"),
                        user != null ? user.getEmail() : null,
                        user != null && user.getProfile() != null ? user.getProfile().getPhone() : null,
                        amount,
                        booking.getRefundMethod() != null ? booking.getRefundMethod() : "CINEWALLET",
                        booking.getRefundedAt() != null ? booking.getRefundedAt() : booking.getUpdatedAt(),
                        seatLabels
                ));
            }

            totalRefundedBookings += refundedBookings.size();
            totalRefundedAmount = totalRefundedAmount.add(showtimeRefundTotal);

            incidentItems.add(new ShowtimeIncidentItem(
                    showtime.getId(),
                    showtime.getMovie() != null ? showtime.getMovie().getTitle() : "Phim",
                    showtime.getRoom() != null ? showtime.getRoom().getName() : "Phòng",
                    showtime.getRoom() != null && showtime.getRoom().getCinema() != null ? showtime.getRoom().getCinema().getName() : "Rạp",
                    showtime.getStartTime(),
                    showtime.getCancellationReason() != null ? showtime.getCancellationReason() : "Suất chiếu bị hủy do sự cố rạp",
                    showtime.getCancelledAt() != null ? showtime.getCancelledAt() : showtime.getUpdatedAt(),
                    refundedBookings.size(),
                    showtimeRefundTotal,
                    refundedUsers
            ));
        }

        return new ShowtimeIncidentReportResponse(
                cancelledShowtimes.size(),
                totalRefundedBookings,
                totalRefundedAmount,
                uniqueUserIds.size(),
                incidentItems
        );
    }
}
