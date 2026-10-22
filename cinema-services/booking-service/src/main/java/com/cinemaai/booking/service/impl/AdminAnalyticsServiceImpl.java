package com.cinemaai.booking.service.impl;

import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.dto.ReportingShowtimeDto;
import com.cinemaai.booking.dto.response.report.*;
import com.cinemaai.booking.dto.response.report.AdminAnalyticsResponse.*;
import com.cinemaai.booking.entity.*;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.OrderRepository;
import com.cinemaai.booking.service.AdminAnalyticsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {
    private final BookingRepository bookingRepository;
    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;

    private record Range(LocalDate from, LocalDate to) {
        boolean contains(LocalDateTime time) {
            return time != null && !time.isBefore(from.atStartOfDay()) && time.isBefore(to.plusDays(1).atStartOfDay());
        }
    }
    private Range range(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1) : from;
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) >= 366) {
            throw new BadRequestException("Khoảng báo cáo phải từ 1 đến 366 ngày");
        }
        return new Range(start, end);
    }
    private static BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static boolean paid(Booking b) { return b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.USED; }
    private static boolean paid(Order o) { return "PAID".equals(o.getStatus()) || "PICKED_UP".equals(o.getStatus()); }
    private static LocalDateTime paidAt(Booking b) { return b.getPaidAt() != null ? b.getPaidAt() : b.getCreatedAt(); }
    private static LocalDateTime paidAt(Order o) { return o.getPaidAt() != null ? o.getPaidAt() : o.getCreatedAt(); }
    private static double percent(long numerator, long denominator) {
        return denominator == 0 ? 0 : Math.round(1000.0 * numerator / denominator) / 10.0;
    }
    private static BigDecimal foodAmount(Booking b) {
        BigDecimal food = b.getFoodItems().stream().map(f -> money(f.getLineTotal())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal seats = b.getSeats().stream().map(s -> money(s.getUnitPrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gross = food.add(seats);
        if (gross.signum() == 0 || food.signum() == 0) return BigDecimal.ZERO;
        // Allocate the amount actually paid (including any loyalty discount) between tickets and food.
        return money(b.getTotalAmount()).multiply(food).divide(gross, 2, RoundingMode.HALF_UP);
    }
    private static final class Bucket {
        BigDecimal ticket = BigDecimal.ZERO, food = BigDecimal.ZERO;
        long tickets, transactions, quantity, shows, capacity, occupied;
        BigDecimal total() { return ticket.add(food); }
    }
    private record FoodLine(String name, long quantity, BigDecimal gross) {}
    private record FoodSale(LocalDate date, String source, BigDecimal amount, List<FoodLine> lines) {}

    private List<FoodSale> foodSales(List<Booking> bookings, List<Order> orders, Range range) {
        List<FoodSale> result = new ArrayList<>();
        for (Booking b : bookings) {
            if (paid(b) && range.contains(paidAt(b)) && !b.getFoodItems().isEmpty()) {
                result.add(new FoodSale(paidAt(b).toLocalDate(), "WITH_TICKET", foodAmount(b), b.getFoodItems().stream()
                        .map(f -> new FoodLine(f.getProductNameSnapshot(), f.getQuantity(), money(f.getLineTotal()))).toList()));
            }
        }
        for (Order o : orders) {
            if (paid(o) && range.contains(paidAt(o))) {
                result.add(new FoodSale(paidAt(o).toLocalDate(), o.getBookingId() == null ? "STANDALONE" : "ADD_ON",
                        money(o.getTotalAmount()), o.getItems().stream()
                        .map(f -> new FoodLine(f.getProductNameSnapshot(), f.getQuantity(), money(f.getLineTotal()))).toList()));
            }
        }
        return result;
    }

    private ConcessionSalesResponse concessions(List<Booking> bookings, List<Order> orders, Range range) {
        Map<LocalDate, Bucket> daily = new TreeMap<>();
        for (LocalDate d = range.from; !d.isAfter(range.to); d = d.plusDays(1)) daily.put(d, new Bucket());
        Map<String, Bucket> products = new HashMap<>(), sources = new LinkedHashMap<>();
        for (String source : List.of("WITH_TICKET", "ADD_ON", "STANDALONE")) sources.put(source, new Bucket());
        Bucket total = new Bucket();
        for (FoodSale sale : foodSales(bookings, orders, range)) {
            long quantity = sale.lines.stream().mapToLong(FoodLine::quantity).sum();
            total.food = total.food.add(sale.amount); total.transactions++; total.quantity += quantity;
            for (Bucket bucket : List.of(daily.get(sale.date), sources.get(sale.source))) {
                bucket.food = bucket.food.add(sale.amount); bucket.transactions++; bucket.quantity += quantity;
            }
            BigDecimal gross = sale.lines.stream().map(FoodLine::gross).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal allocated = BigDecimal.ZERO;
            for (int i = 0; i < sale.lines.size(); i++) {
                FoodLine line = sale.lines.get(i);
                BigDecimal amount = i == sale.lines.size() - 1 ? sale.amount.subtract(allocated)
                        : gross.signum() == 0 ? BigDecimal.ZERO : sale.amount.multiply(line.gross).divide(gross, 2, RoundingMode.HALF_UP);
                allocated = allocated.add(amount);
                Bucket product = products.computeIfAbsent(Objects.requireNonNullElse(line.name, "Món F&B"), key -> new Bucket());
                product.quantity += line.quantity; product.food = product.food.add(amount);
            }
        }
        return new ConcessionSalesResponse(range.from, range.to, total.transactions, total.quantity, total.food,
                total.transactions == 0 ? BigDecimal.ZERO : total.food.divide(BigDecimal.valueOf(total.transactions), 2, RoundingMode.HALF_UP),
                daily.entrySet().stream().map(e -> new ConcessionSalesResponse.DailyLine(e.getKey(), e.getValue().transactions, e.getValue().quantity, e.getValue().food)).toList(),
                products.entrySet().stream().map(e -> new ConcessionSalesResponse.Line(e.getKey(), e.getValue().quantity, e.getValue().food))
                        .sorted(Comparator.comparing(ConcessionSalesResponse.Line::revenue).reversed().thenComparing(ConcessionSalesResponse.Line::name)).toList(),
                sources.entrySet().stream().map(e -> new ConcessionSalesResponse.SourceLine(e.getKey(), e.getValue().transactions, e.getValue().quantity, e.getValue().food)).toList(),
                null, null, null, null);
    }

    @Override
    public ConcessionSalesResponse getConcessions(LocalDate from, LocalDate to, Long cinemaId) {
        Range r = range(from, to);
        return concessions(bookingRepository.findForAnalytics(r.from.atStartOfDay(), r.to.plusDays(1).atStartOfDay(), cinemaId),
                orderRepository.findForAnalytics(r.from.atStartOfDay(), r.to.plusDays(1).atStartOfDay(), cinemaId), r);
    }

    @Override
    public AdminAnalyticsResponse getAnalytics(LocalDate from, LocalDate to, Long cinemaId) {
        Range r = range(from, to);
        List<Booking> bookings = bookingRepository.findForAnalytics(r.from.atStartOfDay(), r.to.plusDays(1).atStartOfDay(), cinemaId);
        List<Order> orders = orderRepository.findForAnalytics(r.from.atStartOfDay(), r.to.plusDays(1).atStartOfDay(), cinemaId);
        // Capacity comes from Catalog; a connection failure is surfaced instead of manufacturing occupancy.
        List<ReportingShowtimeDto> shows = catalogClient.getReportingShowtimes(r.from, r.to, cinemaId);
        Map<Long, ReportingShowtimeDto> showMap = shows.stream().collect(Collectors.toMap(ReportingShowtimeDto::showtimeId, s -> s));
        Map<Long, Bucket> cinemas = new TreeMap<>(), movies = new HashMap<>(), rooms = new TreeMap<>();
        Map<Long, String> cinemaNames = new HashMap<>(), movieNames = new HashMap<>(), roomNames = new HashMap<>();
        Map<LocalDate, Bucket> daily = new TreeMap<>(), occupancy = new TreeMap<>();
        Map<Long, Long> soldByShow = bookings.stream().filter(AdminAnalyticsServiceImpl::paid)
                .collect(Collectors.groupingBy(Booking::getShowtimeId, Collectors.summingLong(b -> b.getSeats().size())));
        Map<String, Long> statuses = new LinkedHashMap<>();
        for (BookingStatus status : BookingStatus.values()) statuses.put(status.name(), 0L);
        long[] peaks = new long[24];
        Bucket total = new Bucket();
        for (LocalDate d = r.from; !d.isAfter(r.to); d = d.plusDays(1)) { daily.put(d, new Bucket()); occupancy.put(d, new Bucket()); }
        for (Booking b : bookings) {
            Long cid = Objects.requireNonNullElse(b.getCinemaId(), 0L);
            cinemaNames.putIfAbsent(cid, Objects.requireNonNullElse(b.getCinemaNameSnapshot(), "Chưa xác định rạp"));
            if (r.contains(b.getCreatedAt())) statuses.merge(b.getStatus().name(), 1L, Long::sum);
            if (!paid(b) || !r.contains(paidAt(b))) continue;
            BigDecimal food = foodAmount(b), ticket = money(b.getTotalAmount()).subtract(food);
            Bucket day = daily.get(paidAt(b).toLocalDate());
            Bucket cinema = cinemas.computeIfAbsent(cid, key -> new Bucket());
            for (Bucket bucket : List.of(total, day, cinema)) {
                bucket.ticket = bucket.ticket.add(ticket); bucket.food = bucket.food.add(food);
                bucket.tickets += b.getSeats().size(); bucket.transactions++;
            }
            Bucket movie = movies.computeIfAbsent(b.getMovieId(), key -> new Bucket());
            movie.ticket = movie.ticket.add(ticket); movie.tickets += b.getSeats().size();
            movieNames.putIfAbsent(b.getMovieId(), b.getMovieTitleSnapshot());
        }
        for (Order o : orders) {
            if (!paid(o) || !r.contains(paidAt(o))) continue;
            Long cid = Objects.requireNonNullElse(o.getCinemaId(), 0L);
            cinemaNames.putIfAbsent(cid, Objects.requireNonNullElse(o.getCinemaName(), "Chưa xác định rạp"));
            for (Bucket bucket : List.of(total, daily.get(paidAt(o).toLocalDate()), cinemas.computeIfAbsent(cid, key -> new Bucket()))) {
                bucket.food = bucket.food.add(money(o.getTotalAmount())); bucket.transactions++;
            }
        }
        List<ShowtimeFillResponse> fill = new ArrayList<>();
        for (ReportingShowtimeDto s : shows) {
            long sold = soldByShow.getOrDefault(s.showtimeId(), 0L);
            cinemaNames.put(s.cinemaId(), s.cinemaName());
            roomNames.put(s.roomId(), s.roomName());
            for (Bucket bucket : List.of(total, cinemas.computeIfAbsent(s.cinemaId(), key -> new Bucket()),
                    rooms.computeIfAbsent(s.roomId(), key -> new Bucket()), occupancy.get(s.startTime().toLocalDate()))) {
                bucket.shows++; bucket.capacity += s.capacity(); bucket.occupied += sold;
            }
            peaks[s.startTime().getHour()] += sold;
            fill.add(new ShowtimeFillResponse(s.showtimeId(), s.movieTitle(), s.roomName(), s.startTime(), s.capacity(), sold, Math.max(0, s.capacity() - sold)));
        }
        LocalDateTime now = LocalDateTime.now();
        List<Booking> finished = bookings.stream().filter(AdminAnalyticsServiceImpl::paid).filter(b -> r.contains(b.getShowtimeStartSnapshot()))
                .filter(b -> {
                    LocalDateTime end = b.getShowtimeEndSnapshot();
                    if (end == null && showMap.containsKey(b.getShowtimeId())) end = showMap.get(b.getShowtimeId()).endTime();
                    return end != null && end.isBefore(now);
                }).toList();
        long noShowCount = finished.stream().filter(b -> b.getStatus() == BookingStatus.PAID && b.getCheckedInAt() == null).count();
        long expired = statuses.get("EXPIRED"), cancelled = statuses.get("CANCELLED"), settled = statuses.get("PAID") + statuses.get("USED") + statuses.get("REFUNDED");
        List<Booking> refunded = bookings.stream().filter(b -> b.getStatus() == BookingStatus.REFUNDED && r.contains(b.getRefundedAt())).toList();
        ConcessionSalesResponse fnb = concessions(bookings, orders, r);
        List<PeakHourResponse> peakHours = new ArrayList<>();
        for (int h = 0; h < 24; h++) peakHours.add(new PeakHourResponse(h, peaks[h]));
        return new AdminAnalyticsResponse(r.from, r.to, cinemaId,
                new Summary(total.total(), total.ticket, total.food, total.transactions, total.tickets, fnb.totalOrders(),
                        total.shows, total.capacity, percent(total.occupied, total.capacity), refunded.size(),
                        refunded.stream().map(b -> money(b.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add)),
                daily.entrySet().stream().map(e -> new Daily(e.getKey(), e.getValue().ticket, e.getValue().food, e.getValue().total(), e.getValue().tickets, e.getValue().transactions)).toList(),
                cinemas.entrySet().stream().map(e -> { Bucket b = e.getValue(); return new CinemaLine(e.getKey(), cinemaNames.get(e.getKey()),
                        b.ticket, b.food, b.total(), b.tickets, b.transactions, b.shows, b.capacity, percent(b.occupied, b.capacity)); })
                        .sorted(Comparator.comparing(CinemaLine::totalRevenue).reversed()).toList(),
                movies.entrySet().stream().map(e -> new TopMovieResponse(e.getKey(), movieNames.get(e.getKey()), e.getValue().tickets, e.getValue().ticket))
                        .sorted(Comparator.comparing(TopMovieResponse::getRevenue).reversed().thenComparing(TopMovieResponse::getMovieId)).toList(),
                statuses.entrySet().stream().map(e -> new StatusLine(e.getKey(), e.getValue())).toList(), peakHours,
                rooms.entrySet().stream().map(e -> { Bucket b = e.getValue(); return new RoomOccupancyResponse(e.getKey(), roomNames.get(e.getKey()), b.occupied, b.capacity, percent(b.occupied, b.capacity), b.shows); }).toList(),
                occupancy.entrySet().stream().map(e -> { Bucket b = e.getValue(); return new DailyOccupancyResponse(e.getKey(), b.occupied, b.capacity, b.shows, percent(b.occupied, b.capacity)); }).toList(),
                fill.stream().sorted(Comparator.comparing(ShowtimeFillResponse::startTime)).toList(),
                new NoShowReportResponse(r.from, r.to, noShowCount, finished.size(), percent(noShowCount, finished.size())),
                new AbandonedRateResponse(r.from, r.to, expired, cancelled, settled, percent(expired, expired + cancelled + settled)), fnb);
    }
}
