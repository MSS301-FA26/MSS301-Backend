package com.cinemaai.booking.service.impl;

import com.cinemaai.booking.client.CatalogClient;
import com.cinemaai.booking.client.dto.CatalogQuoteDto;
import com.cinemaai.booking.dto.request.CheckoutBookingRequest;
import com.cinemaai.booking.dto.request.HoldSeatsRequest;
import com.cinemaai.booking.dto.request.UpdateHoldingBookingRequest;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingFoodItem;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.entity.BookingTicket;
import com.cinemaai.booking.enums.BookingSeatStatus;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.enums.SeatType;
import com.cinemaai.booking.enums.TicketType;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.exception.ConflictException;
import com.cinemaai.booking.exception.NotFoundException;
import com.cinemaai.booking.mapper.BookingMapper;
import com.cinemaai.booking.repository.BookingFoodItemRepository;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.BookingSeatRepository;
import com.cinemaai.booking.repository.BookingTicketRepository;
import com.cinemaai.booking.service.BookingService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingTicketRepository bookingTicketRepository;
    private final BookingFoodItemRepository bookingFoodItemRepository;
    private final CatalogClient catalogClient;

    @Override
    @Transactional
    public BookingResponse holdSeats(Long userId, HoldSeatsRequest request) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Race Condition check: Pessimistic Row Locking on requested seats
        List<Long> conflicts = bookingSeatRepository.findConflictedSeatIds(
                request.showtimeId(), request.seatIds(), now);

        if (!conflicts.isEmpty()) {
            throw new ConflictException("Một hoặc nhiều ghế đã có người giữ hoặc đã được bán. Vui lòng chọn ghế khác.");
        }

        // 2. Release any previous active HOLDING booking for this user & showtime
        List<Booking> existingHolds = bookingRepository.findByUserIdAndShowtimeIdAndStatusIn(
                userId, request.showtimeId(), List.of(BookingStatus.HOLDING));
        for (Booking oldHold : existingHolds) {
            oldHold.setStatus(BookingStatus.CANCELLED);
            oldHold.setCancelledAt(now);
            for (BookingSeat seat : oldHold.getSeats()) {
                seat.setStatus(BookingSeatStatus.RELEASED);
            }
            bookingRepository.save(oldHold);
        }

        // 3. Prepare quote request to fetch Authoritative metadata & pricing from Catalog
        List<CatalogQuoteDto.Request.Ticket> quoteTickets = new ArrayList<>();
        if (request.tickets() != null && !request.tickets().isEmpty()) {
            for (HoldSeatsRequest.TicketSelection t : request.tickets()) {
                quoteTickets.add(new CatalogQuoteDto.Request.Ticket(
                        t.seatId(),
                        t.ticketType() == null ? "ADULT" : t.ticketType().toUpperCase(),
                        t.viewerAge() == null ? 20 : t.viewerAge(),
                        t.quantity() == null ? 1 : t.quantity()
                ));
            }
        } else {
            // Default: 1 ADULT ticket per selected seat
            for (Long seatId : request.seatIds()) {
                quoteTickets.add(new CatalogQuoteDto.Request.Ticket(seatId, "ADULT", 20, 1));
            }
        }

        List<CatalogQuoteDto.Request.Food> quoteFoods = new ArrayList<>();
        if (request.foods() != null) {
            for (HoldSeatsRequest.FoodSelection f : request.foods()) {
                quoteFoods.add(new CatalogQuoteDto.Request.Food(
                        f.productId(),
                        Boolean.TRUE.equals(f.isCombo()),
                        f.quantity() == null ? 1 : f.quantity()
                ));
            }
        }

        CatalogQuoteDto.Request quoteReq = new CatalogQuoteDto.Request(
                request.showtimeId(),
                request.seatIds(),
                quoteTickets,
                quoteFoods
        );

        CatalogQuoteDto.Response quote = catalogClient.getQuote(quoteReq);

        // 4. Create new Booking Entity with Snapshots
        String bookingCode = "BK" + now.format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + String.format("%04d", new Random().nextInt(10000));

        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .userId(userId)
                .showtimeId(request.showtimeId())
                .movieId(quote.showtime().movieId())
                .movieTitleSnapshot(quote.showtime().movieTitle())
                .moviePosterSnapshot(quote.showtime().posterUrl())
                .cinemaNameSnapshot(quote.showtime().cinemaName())
                .roomNameSnapshot(quote.showtime().roomName())
                .showtimeStartSnapshot(quote.showtime().startTime())
                .subtotal(quote.subtotal())
                .discountAmount(BigDecimal.ZERO)
                .loyaltyPointsRedeemed(request.loyaltyPointsToRedeem() != null ? request.loyaltyPointsToRedeem() : 0)
                .totalAmount(quote.subtotal())
                .status(BookingStatus.HOLDING)
                .holdExpiresAt(now.plusMinutes(3))
                .build();

        booking = bookingRepository.save(booking);

        // 5. Save BookingSeats
        List<BookingSeat> seats = new ArrayList<>();
        Map<Long, CatalogQuoteDto.Response.SeatSnapshot> seatMap = new HashMap<>();
        for (CatalogQuoteDto.Response.SeatSnapshot s : quote.seats()) {
            seatMap.put(s.seatId(), s);
        }

        for (Long seatId : request.seatIds()) {
            CatalogQuoteDto.Response.SeatSnapshot sSnapshot = seatMap.get(seatId);
            String label = sSnapshot != null ? sSnapshot.seatLabel() : "S" + seatId;
            String row = label.replaceAll("\\d+", "");
            int num = 0;
            try {
                num = Integer.parseInt(label.replaceAll("\\D+", ""));
            } catch (Exception ignored) {}

            SeatType sType = SeatType.STANDARD;
            if (sSnapshot != null && sSnapshot.seatType() != null) {
                try {
                    sType = SeatType.valueOf(sSnapshot.seatType().toUpperCase());
                } catch (Exception ignored) {}
            }

            BigDecimal price = sSnapshot != null && sSnapshot.unitPrice() != null ? sSnapshot.unitPrice() : BigDecimal.ZERO;

            BookingSeat seat = BookingSeat.builder()
                    .booking(booking)
                    .showtimeId(request.showtimeId())
                    .seatId(seatId)
                    .rowLabel(row.isBlank() ? "A" : row)
                    .seatNumber(num)
                    .seatType(sType)
                    .unitPrice(price)
                    .status(BookingSeatStatus.HOLDING)
                    .ticketType(TicketType.ADULT)
                    .build();
            seats.add(seat);
        }

        if (booking.getSeats() == null) {
            booking.setSeats(new ArrayList<>());
        } else {
            booking.getSeats().clear();
        }
        booking.getSeats().addAll(seats);

        // 6. Save Tickets & Foods if provided
        populateTicketsAndFoods(booking, quote);
        booking = bookingRepository.save(booking);

        return BookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse checkout(Long userId, Long bookingId, CheckoutBookingRequest request) {
        return processCheckoutOrUpdate(userId, bookingId, request.tickets(), request.foods(), request.loyaltyPointsToRedeem(), true);
    }

    @Override
    @Transactional
    public BookingResponse updateHoldingItems(Long userId, Long bookingId, UpdateHoldingBookingRequest request) {
        return processCheckoutOrUpdate(userId, bookingId, request.tickets(), request.foods(), request.loyaltyPointsToRedeem(), false);
    }

    private BookingResponse processCheckoutOrUpdate(
            Long userId,
            Long bookingId,
            List<HoldSeatsRequest.TicketSelection> tickets,
            List<HoldSeatsRequest.FoodSelection> foods,
            Integer loyaltyPointsToRedeem,
            boolean isCheckoutFinal
    ) {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn đặt vé."));

        if (booking.getStatus() != BookingStatus.HOLDING && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Đơn đặt vé không ở trạng thái hợp lệ để cập nhật.");
        }

        if (booking.getHoldExpiresAt() != null && booking.getHoldExpiresAt().isBefore(now)) {
            booking.setStatus(BookingStatus.EXPIRED);
            for (BookingSeat seat : booking.getSeats()) {
                seat.setStatus(BookingSeatStatus.RELEASED);
            }
            bookingRepository.save(booking);
            throw new BadRequestException("Thời gian giữ ghế đã hết hạn. Vui lòng thực hiện đặt lại.");
        }

        List<Long> seatIds = booking.getSeats().stream().map(BookingSeat::getSeatId).toList();

        // Prepare quote request
        List<CatalogQuoteDto.Request.Ticket> quoteTickets = new ArrayList<>();
        if (tickets != null && !tickets.isEmpty()) {
            for (HoldSeatsRequest.TicketSelection t : tickets) {
                quoteTickets.add(new CatalogQuoteDto.Request.Ticket(
                        t.seatId(),
                        t.ticketType() == null ? "ADULT" : t.ticketType().toUpperCase(),
                        t.viewerAge() == null ? 20 : t.viewerAge(),
                        t.quantity() == null ? 1 : t.quantity()
                ));
            }
        } else {
            for (Long seatId : seatIds) {
                quoteTickets.add(new CatalogQuoteDto.Request.Ticket(seatId, "ADULT", 20, 1));
            }
        }

        List<CatalogQuoteDto.Request.Food> quoteFoods = new ArrayList<>();
        if (foods != null) {
            for (HoldSeatsRequest.FoodSelection f : foods) {
                quoteFoods.add(new CatalogQuoteDto.Request.Food(
                        f.productId(),
                        Boolean.TRUE.equals(f.isCombo()),
                        f.quantity() == null ? 1 : f.quantity()
                ));
            }
        }

        CatalogQuoteDto.Request quoteReq = new CatalogQuoteDto.Request(
                booking.getShowtimeId(),
                seatIds,
                quoteTickets,
                quoteFoods
        );

        CatalogQuoteDto.Response quote = catalogClient.getQuote(quoteReq);

        // Update snapshot fields from quote
        booking.setMovieTitleSnapshot(quote.showtime().movieTitle());
        booking.setMoviePosterSnapshot(quote.showtime().posterUrl());
        booking.setCinemaNameSnapshot(quote.showtime().cinemaName());
        booking.setRoomNameSnapshot(quote.showtime().roomName());
        booking.setShowtimeStartSnapshot(quote.showtime().startTime());
        booking.setSubtotal(quote.subtotal());

        // Calculate discount if loyalty points redeemed (1 point = 1,000 VND example or configurable)
        int points = loyaltyPointsToRedeem != null ? loyaltyPointsToRedeem : booking.getLoyaltyPointsRedeemed();
        BigDecimal discount = BigDecimal.valueOf(points).multiply(BigDecimal.valueOf(1000));
        if (discount.compareTo(quote.subtotal()) > 0) {
            discount = quote.subtotal();
        }
        booking.setDiscountAmount(discount);
        booking.setLoyaltyPointsRedeemed(points);
        booking.setTotalAmount(quote.subtotal().subtract(discount).max(BigDecimal.ZERO));

        if (isCheckoutFinal) {
            booking.setStatus(BookingStatus.PENDING_PAYMENT);
        }

        populateTicketsAndFoods(booking, quote);

        bookingRepository.save(booking);
        return BookingMapper.toResponse(booking);
    }

    private void populateTicketsAndFoods(Booking booking, CatalogQuoteDto.Response quote) {
        if (booking.getTickets() == null) {
            booking.setTickets(new ArrayList<>());
        } else {
            booking.getTickets().clear();
        }

        if (quote.tickets() != null) {
            for (CatalogQuoteDto.Response.TicketSnapshot ts : quote.tickets()) {
                TicketType tType = TicketType.ADULT;
                try {
                    tType = TicketType.valueOf(ts.ticketType().toUpperCase());
                } catch (Exception ignored) {}

                booking.getTickets().add(BookingTicket.builder()
                        .booking(booking)
                        .seatId(ts.seatId())
                        .ticketType(tType)
                        .viewerAge(20)
                        .quantity(ts.quantity())
                        .unitPrice(ts.unitPrice())
                        .lineTotal(ts.lineTotal())
                        .build());
            }
        }

        if (booking.getFoodItems() == null) {
            booking.setFoodItems(new ArrayList<>());
        } else {
            booking.getFoodItems().clear();
        }

        if (quote.foods() != null) {
            for (CatalogQuoteDto.Response.FoodSnapshot fs : quote.foods()) {
                booking.getFoodItems().add(BookingFoodItem.builder()
                        .booking(booking)
                        .productId(fs.productId())
                        .isCombo(fs.isCombo())
                        .productNameSnapshot(fs.productName())
                        .quantity(fs.quantity())
                        .unitPrice(fs.unitPrice())
                        .lineTotal(fs.lineTotal())
                        .build());
            }
        }
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn đặt vé."));

        if (booking.getStatus() != BookingStatus.HOLDING && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Chỉ có thể hủy đơn đặt vé đang trong phiên giữ chỗ hoặc chờ thanh toán.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        for (BookingSeat seat : booking.getSeats()) {
            seat.setStatus(BookingSeatStatus.RELEASED);
        }
        bookingRepository.save(booking);
        return BookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(Long userId, Pageable pageable) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(BookingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn đặt vé."));
        return BookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByIdInternal(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn đặt vé."));
        return BookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByCode(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mã đặt vé: " + bookingCode));
        return BookingMapper.toResponse(booking);
    }
}
