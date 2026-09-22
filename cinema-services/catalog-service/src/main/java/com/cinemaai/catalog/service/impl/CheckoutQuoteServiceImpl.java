package com.cinemaai.catalog.service.impl;

import com.cinemaai.catalog.dto.request.quote.CheckoutQuoteRequest;
import com.cinemaai.catalog.dto.response.quote.CheckoutQuoteResponse;
import com.cinemaai.catalog.dto.response.quote.CheckoutQuoteResponse.*;
import com.cinemaai.catalog.entity.Seat;
import com.cinemaai.catalog.enums.*;
import com.cinemaai.catalog.exception.BadRequestException;
import com.cinemaai.catalog.repository.*;
import com.cinemaai.catalog.service.CheckoutQuoteService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckoutQuoteServiceImpl implements CheckoutQuoteService {
    private final ShowtimeRepository showtimes;
    private final SeatRepository seats;
    private final FoodItemRepository items;
    private final FoodComboRepository combos;
    @Value("${app.quote.ttl-seconds}") private long ttlSeconds;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CheckoutQuoteResponse quote(CheckoutQuoteRequest request) {
        var showtime = showtimes.findWithDetailsById(request.showtimeId())
                .orElseThrow(() -> new BadRequestException("Showtime does not exist"));
        if (showtime.getStatus() != ShowtimeStatus.OPEN || !showtime.getStartTime().isAfter(LocalDateTime.now()))
            throw new BadRequestException("Showtime is not open for booking");
        if (showtime.getMovie().getStatus() == MovieStatus.INACTIVE || showtime.getRoom().getStatus() != RoomStatus.ACTIVE
                || showtime.getRoom().getCinema().getStatus() != CinemaStatus.ACTIVE)
            throw new BadRequestException("Movie, room or cinema is unavailable");
        Set<Long> requested = new LinkedHashSet<>(request.seatIds());
        if (requested.size() != request.seatIds().size()) throw new BadRequestException("Duplicate seat IDs");
        Map<Long, Seat> found = seats.findAllById(requested).stream().collect(Collectors.toMap(Seat::getId, Function.identity()));
        for (Long id : requested) {
            Seat seat = found.get(id);
            if (seat == null) throw new BadRequestException("Seat does not exist: " + id);
            if (!seat.getRoom().getId().equals(showtime.getRoom().getId())) throw new BadRequestException("Seat belongs to another room: " + id);
            if (seat.getStatus() != SeatStatus.AVAILABLE) throw new BadRequestException("Seat is unavailable: " + id);
        }
        if (request.tickets().stream().mapToLong(CheckoutQuoteRequest.Ticket::quantity).sum() != requested.size())
            throw new BadRequestException("Ticket quantity must match the number of selected seats");
        // Explicit assignments are reserved first, so subsequent batches cannot reuse them.
        Set<Long> assigned = new HashSet<>();
        for (var ticket : request.tickets()) {
            if (ticket.seatId() != null && (ticket.quantity() != 1 || !requested.contains(ticket.seatId()) || !assigned.add(ticket.seatId())))
                throw new BadRequestException("Invalid or duplicate ticket seat assignment");
        }
        Iterator<Long> unassigned = requested.stream().filter(id -> !assigned.contains(id)).iterator();
        List<SeatSnapshot> seatLines = new ArrayList<>();
        List<TicketSnapshot> ticketLines = new ArrayList<>();
        BigDecimal ticketTotal = BigDecimal.ZERO;
        for (var ticket : request.tickets()) {
            if (!ticket.ticketType().allowsAge(ticket.viewerAge()) || (showtime.getMovie().getAgeRating() != null
                    && !showtime.getMovie().getAgeRating().allowsAge(ticket.viewerAge())))
                throw new BadRequestException("Viewer age is not eligible for ticket type or movie age rating");
            for (int i = 0; i < ticket.quantity(); i++) {
                Seat seat = found.get(ticket.seatId() != null ? ticket.seatId() : unassigned.next());
                BigDecimal price = showtime.getPriceForTicketAndSeatType(ticket.ticketType(), seat.getSeatType());
                if (price == null || price.signum() < 0) throw new BadRequestException("Ticket price is not configured");
                seatLines.add(new SeatSnapshot(seat.getId(), seat.getRowLabel() + String.format("%02d", seat.getSeatNumber()), seat.getSeatType(), price));
                ticketLines.add(new TicketSnapshot(seat.getId(), ticket.ticketType(), 1, price, price));
                ticketTotal = ticketTotal.add(price);
            }
        }
        List<FoodSnapshot> foodLines = new ArrayList<>();
        Set<String> foodKeys = new HashSet<>();
        BigDecimal foodTotal = BigDecimal.ZERO;
        for (var food : request.foods()) {
            if (!foodKeys.add(food.isCombo() + ":" + food.productId())) throw new BadRequestException("Duplicate food product");
            String name;
            BigDecimal price;
            FoodItemStatus status;
            if (food.isCombo()) {
                var combo = combos.findById(food.productId()).orElseThrow(() -> new BadRequestException("Food combo does not exist: " + food.productId()));
                name = combo.getName(); price = combo.getPrice(); status = combo.getStatus();
            } else {
                var item = items.findById(food.productId()).orElseThrow(() -> new BadRequestException("Food item does not exist: " + food.productId()));
                name = item.getName(); price = item.getPrice(); status = item.getStatus();
            }
            if (status != FoodItemStatus.ACTIVE) throw new BadRequestException("Food product is unavailable: " + food.productId());
            BigDecimal total = price.multiply(BigDecimal.valueOf(food.quantity()));
            foodLines.add(new FoodSnapshot(food.productId(), food.isCombo(), name, price, food.quantity(), total));
            foodTotal = foodTotal.add(total);
        }
        var movie = showtime.getMovie();
        var room = showtime.getRoom();
        return new CheckoutQuoteResponse(UUID.randomUUID().toString(), Instant.now().plusSeconds(ttlSeconds),
                new ShowtimeSnapshot(showtime.getId(), movie.getId(), movie.getTitle(), movie.getPosterUrl(), room.getCinema().getName(), room.getName(), showtime.getStartTime()),
                List.copyOf(seatLines), List.copyOf(ticketLines), List.copyOf(foodLines), ticketTotal, foodTotal, ticketTotal.add(foodTotal));
    }
}
