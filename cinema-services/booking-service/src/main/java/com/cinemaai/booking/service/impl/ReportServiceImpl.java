package com.cinemaai.booking.service.impl;

import com.cinemaai.booking.dto.response.report.*;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingFoodItem;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.repository.BookingFoodItemRepository;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.BookingSeatRepository;
import com.cinemaai.booking.repository.FoodOrderRepository;
import com.cinemaai.booking.service.ReportService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingFoodItemRepository bookingFoodItemRepository;
    private final FoodOrderRepository foodOrderRepository;

    private List<Booking> getBookingsInRange(LocalDate from, LocalDate to) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate safeTo = to != null ? to : LocalDate.now();
        LocalDateTime dtFrom = safeFrom.atStartOfDay();
        LocalDateTime dtTo = safeTo.plusDays(1).atStartOfDay();

        return bookingRepository.findAll().stream()
                .filter(b -> b.getCreatedAt() != null && !b.getCreatedAt().isBefore(dtFrom) && !b.getCreatedAt().isAfter(dtTo))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenue(LocalDate from, LocalDate to) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate safeTo = to != null ? to : LocalDate.now();

        List<Booking> inRange = getBookingsInRange(safeFrom, safeTo);
        List<Booking> paid = inRange.stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.USED)
                .toList();

        BigDecimal totalRevenue = paid.stream()
                .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long transactions = paid.size();
        long ticketsSold = paid.stream()
                .mapToLong(b -> b.getSeats() != null ? b.getSeats().size() : 0)
                .sum();

        List<ProviderRevenueResponse> byProvider = List.of(
                new ProviderRevenueResponse("CINEWALLET", totalRevenue, transactions)
        );

        return new RevenueReportResponse(safeFrom, safeTo, totalRevenue, transactions, ticketsSold, byProvider);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopMovieResponse> getTopMovies(LocalDate from, LocalDate to, int limit) {
        List<Booking> paid = getBookingsInRange(from, to).stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.USED)
                .toList();

        Map<Long, List<Booking>> byMovie = paid.stream()
                .filter(b -> b.getMovieId() != null)
                .collect(Collectors.groupingBy(Booking::getMovieId));

        int safeLimit = Math.max(1, Math.min(limit, 50));

        return byMovie.entrySet().stream()
                .map(e -> {
                    Long movieId = e.getKey();
                    List<Booking> list = e.getValue();
                    String title = list.stream()
                            .map(Booking::getMovieTitleSnapshot)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse("Phim #" + movieId);

                    long tickets = list.stream()
                            .mapToLong(b -> b.getSeats() != null ? b.getSeats().size() : 0)
                            .sum();

                    BigDecimal rev = list.stream()
                            .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new TopMovieResponse(movieId, title, tickets, rev);
                })
                .sorted(Comparator.comparing(TopMovieResponse::getTicketsSold).reversed())
                .limit(safeLimit)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomOccupancyResponse> getRoomOccupancy(LocalDate from, LocalDate to) {
        List<Booking> paid = getBookingsInRange(from, to).stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.USED)
                .toList();

        Map<String, List<Booking>> byRoom = paid.stream()
                .filter(b -> b.getRoomNameSnapshot() != null)
                .collect(Collectors.groupingBy(Booking::getRoomNameSnapshot));

        long dummyId = 1L;
        List<RoomOccupancyResponse> result = new ArrayList<>();
        for (var entry : byRoom.entrySet()) {
            String roomName = entry.getKey();
            List<Booking> list = entry.getValue();
            long sold = list.stream().mapToLong(b -> b.getSeats() != null ? b.getSeats().size() : 0).sum();
            long showtimes = list.stream().map(Booking::getShowtimeId).filter(Objects::nonNull).distinct().count();
            long capacity = Math.max(sold, showtimes * 50);
            double rate = capacity > 0 ? (double) sold / capacity * 100.0 : 0.0;
            result.add(new RoomOccupancyResponse(dummyId++, roomName, sold, capacity, Math.min(rate, 100.0), showtimes));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyOccupancyResponse> getDailyOccupancy(LocalDate from, LocalDate to) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate safeTo = to != null ? to : LocalDate.now();

        List<Booking> paid = getBookingsInRange(safeFrom, safeTo).stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.USED)
                .toList();

        Map<LocalDate, List<Booking>> byDay = paid.stream()
                .filter(b -> b.getCreatedAt() != null)
                .collect(Collectors.groupingBy(b -> b.getCreatedAt().toLocalDate(), TreeMap::new, Collectors.toList()));

        List<DailyOccupancyResponse> result = new ArrayList<>();
        for (LocalDate d = safeFrom; !d.isAfter(safeTo); d = d.plusDays(1)) {
            List<Booking> list = byDay.getOrDefault(d, List.of());
            long sold = list.stream().mapToLong(b -> b.getSeats() != null ? b.getSeats().size() : 0).sum();
            long showtimes = list.stream().map(Booking::getShowtimeId).filter(Objects::nonNull).distinct().count();
            long cap = Math.max(sold, Math.max(1, showtimes) * 50);
            double rate = cap > 0 ? (double) sold / cap * 100.0 : 0.0;
            result.add(new DailyOccupancyResponse(d, sold, cap, showtimes, Math.min(rate, 100.0)));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeFillResponse> getShowtimeFill(LocalDate date, int days) {
        LocalDate safeDate = date != null ? date : LocalDate.now();
        int safeDays = Math.max(1, Math.min(days, 30));
        LocalDate to = safeDate.plusDays(safeDays);

        List<Booking> paid = getBookingsInRange(safeDate, to).stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.USED)
                .toList();

        Map<Long, List<Booking>> byShowtime = paid.stream()
                .filter(b -> b.getShowtimeId() != null)
                .collect(Collectors.groupingBy(Booking::getShowtimeId));

        return byShowtime.entrySet().stream()
                .map(e -> {
                    Long showtimeId = e.getKey();
                    List<Booking> list = e.getValue();
                    String movieTitle = list.stream().map(Booking::getMovieTitleSnapshot).filter(Objects::nonNull).findFirst().orElse("Phim #" + showtimeId);
                    String roomName = list.stream().map(Booking::getRoomNameSnapshot).filter(Objects::nonNull).findFirst().orElse("Phòng");
                    LocalDateTime start = list.stream().map(Booking::getShowtimeStartSnapshot).filter(Objects::nonNull).findFirst().orElse(LocalDateTime.now());
                    long sold = list.stream().mapToLong(b -> b.getSeats() != null ? b.getSeats().size() : 0).sum();
                    long capacity = Math.max(sold, 60);
                    long remaining = Math.max(0, capacity - sold);
                    return new ShowtimeFillResponse(showtimeId, movieTitle, roomName, start, capacity, sold, remaining);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NoShowReportResponse getNoShows(LocalDate from, LocalDate to) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate safeTo = to != null ? to : LocalDate.now();

        List<Booking> inRange = getBookingsInRange(safeFrom, safeTo);
        long paidCount = inRange.stream().filter(b -> b.getStatus() == BookingStatus.PAID).count();
        long usedCount = inRange.stream().filter(b -> b.getStatus() == BookingStatus.USED).count();
        long totalFinished = paidCount + usedCount;
        double rate = totalFinished > 0 ? (double) paidCount / totalFinished * 100.0 : 0.0;

        return new NoShowReportResponse(safeFrom, safeTo, paidCount, totalFinished, rate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeakHourResponse> getPeakHours(LocalDate from, LocalDate to) {
        List<Booking> paid = getBookingsInRange(from, to).stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.USED)
                .toList();

        Map<Integer, Long> byHour = new HashMap<>();
        for (int h = 0; h < 24; h++) byHour.put(h, 0L);

        for (Booking b : paid) {
            LocalDateTime dt = b.getShowtimeStartSnapshot() != null ? b.getShowtimeStartSnapshot() : b.getCreatedAt();
            if (dt != null) {
                int hour = dt.getHour();
                int seats = b.getSeats() != null ? b.getSeats().size() : 1;
                byHour.put(hour, byHour.getOrDefault(hour, 0L) + seats);
            }
        }

        return byHour.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new PeakHourResponse(e.getKey(), e.getValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopSeatResponse> getTopSeats(LocalDate from, LocalDate to, Long roomId) {
        List<Booking> paid = getBookingsInRange(from, to).stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.USED)
                .toList();

        Map<String, Long> seatCounts = new HashMap<>();
        for (Booking b : paid) {
            if (b.getSeats() != null) {
                for (BookingSeat s : b.getSeats()) {
                    String row = s.getRowLabel() != null ? s.getRowLabel() : "A";
                    int num = s.getSeatNumber() > 0 ? s.getSeatNumber() : 1;
                    String key = row + ":" + num;
                    seatCounts.put(key, seatCounts.getOrDefault(key, 0L) + 1);
                }
            }
        }

        return seatCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .map(e -> {
                    String[] parts = e.getKey().split(":");
                    String row = parts[0];
                    int num = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    return new TopSeatResponse(row, num, "STANDARD", e.getValue());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AbandonedRateResponse getAbandonedRate(LocalDate from, LocalDate to) {
        List<Booking> inRange = getBookingsInRange(from, to);
        long paid = inRange.stream().filter(b -> b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.USED).count();
        long expired = inRange.stream().filter(b -> b.getStatus() == BookingStatus.EXPIRED).count();
        long total = paid + expired;
        double rate = total > 0 ? (double) expired / total * 100.0 : 0.0;

        return new AbandonedRateResponse(from, to, expired, paid, total, rate);
    }

    @Override
    @Transactional(readOnly = true)
    public ConcessionSalesResponse getConcessionSales(LocalDate from, LocalDate to) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate safeTo = to != null ? to : LocalDate.now();

        List<Booking> paid = getBookingsInRange(safeFrom, safeTo).stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.USED)
                .toList();

        Map<String, Long> foodQty = new HashMap<>();
        Map<String, BigDecimal> foodRev = new HashMap<>();
        long totalOrders = 0;
        long totalItems = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Booking b : paid) {
            if (b.getFoodItems() != null && !b.getFoodItems().isEmpty()) {
                totalOrders++;
                for (BookingFoodItem f : b.getFoodItems()) {
                    String name = f.getProductNameSnapshot() != null ? f.getProductNameSnapshot() : "Món F&B";
                    int qty = f.getQuantity();
                    BigDecimal line = f.getLineTotal() != null ? f.getLineTotal() : BigDecimal.ZERO;

                    totalItems += qty;
                    totalRevenue = totalRevenue.add(line);
                    foodQty.put(name, foodQty.getOrDefault(name, 0L) + qty);
                    foodRev.put(name, foodRev.getOrDefault(name, BigDecimal.ZERO).add(line));
                }
            }
        }

        List<ConcessionSalesResponse.Line> lines = foodQty.entrySet().stream()
                .map(e -> new ConcessionSalesResponse.Line(e.getKey(), e.getValue(), foodRev.getOrDefault(e.getKey(), BigDecimal.ZERO)))
                .sorted(Comparator.comparing(ConcessionSalesResponse.Line::quantity).reversed())
                .toList();

        BigDecimal aov = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new ConcessionSalesResponse(
                safeFrom, safeTo, totalOrders, totalItems, totalRevenue, aov,
                List.of(), lines, List.of(), 0, 0, totalRevenue.multiply(BigDecimal.valueOf(0.4)), 40.0
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpiredUserResponse> getExpiredUsers(LocalDate from, LocalDate to) {
        List<Booking> inRange = getBookingsInRange(from, to);
        return inRange.stream()
                .filter(b -> b.getStatus() == BookingStatus.EXPIRED && b.getUserId() != null)
                .collect(Collectors.groupingBy(Booking::getUserId))
                .entrySet().stream()
                .map(e -> {
                    BigDecimal lost = e.getValue().stream()
                            .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new ExpiredUserResponse(
                            e.getKey(),
                            "user" + e.getKey() + "@example.com",
                            (long) e.getValue().size(),
                            lost
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeIncidentReportResponse getShowtimeIncidents(LocalDate from, LocalDate to) {
        List<Booking> inRange = getBookingsInRange(from, to);
        List<Booking> cancelled = inRange.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.REFUNDED)
                .toList();

        long totalIncidents = cancelled.stream().map(Booking::getShowtimeId).filter(Objects::nonNull).distinct().count();
        long totalRefundedTickets = cancelled.stream().mapToLong(b -> b.getSeats() != null ? b.getSeats().size() : 0).sum();
        BigDecimal totalRefundAmount = cancelled.stream()
                .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, List<Booking>> byShowtime = cancelled.stream()
                .filter(b -> b.getShowtimeId() != null)
                .collect(Collectors.groupingBy(Booking::getShowtimeId));

        List<ShowtimeIncidentItem> items = byShowtime.entrySet().stream()
                .map(e -> {
                    Long showtimeId = e.getKey();
                    List<Booking> list = e.getValue();
                    String movie = list.stream().map(Booking::getMovieTitleSnapshot).filter(Objects::nonNull).findFirst().orElse("Phim");
                    String room = list.stream().map(Booking::getRoomNameSnapshot).filter(Objects::nonNull).findFirst().orElse("Phòng");
                    String cinema = list.stream().map(Booking::getCinemaNameSnapshot).filter(Objects::nonNull).findFirst().orElse("Rạp");
                    LocalDateTime start = list.stream().map(Booking::getShowtimeStartSnapshot).filter(Objects::nonNull).findFirst().orElse(LocalDateTime.now());
                    long tickets = list.stream().mapToLong(b -> b.getSeats() != null ? b.getSeats().size() : 0).sum();
                    BigDecimal amt = list.stream().map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

                    List<IncidentRefundedUser> users = list.stream()
                            .map(b -> new IncidentRefundedUser(
                                    b.getId(),
                                    b.getBookingCode(),
                                    b.getUserId(),
                                    "Khách hàng #" + b.getUserId(),
                                    "user" + b.getUserId() + "@example.com",
                                    "0900000000",
                                    b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO,
                                    "CINEWALLET",
                                    b.getCancelledAt() != null ? b.getCancelledAt() : b.getCreatedAt(),
                                    b.getSeats() != null ? b.getSeats().stream().map(s -> s.getRowLabel() + s.getSeatNumber()).collect(Collectors.joining(", ")) : ""
                            ))
                            .toList();

                    return new ShowtimeIncidentItem(
                            showtimeId, movie, room, cinema, start,
                            "Sự cố kỹ thuật / Hủy bởi Admin",
                            list.stream().map(Booking::getCancelledAt).filter(Objects::nonNull).findFirst().orElse(LocalDateTime.now()),
                            users.size(), amt, users
                    );
                })
                .toList();

        return new ShowtimeIncidentReportResponse(
                totalIncidents,
                items.size(),
                totalRefundAmount,
                totalRefundedTickets,
                items
        );
    }
}
