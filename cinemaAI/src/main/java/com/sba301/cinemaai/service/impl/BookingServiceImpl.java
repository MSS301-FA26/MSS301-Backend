package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.booking.BookingFoodRequest;
import com.sba301.cinemaai.dto.response.booking.BookingResponse;
import com.sba301.cinemaai.dto.request.booking.CreateBookingRequest;
import com.sba301.cinemaai.dto.request.booking.HoldSeatsRequest;
import com.sba301.cinemaai.dto.response.ticket.TicketLinePriceResponse;
import com.sba301.cinemaai.dto.request.ticket.TicketPriceValidationRequest;
import com.sba301.cinemaai.dto.request.ticket.TicketSelectionRequest;
import com.sba301.cinemaai.dto.response.ticket.TicketPriceValidationResponse;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.BookingFoodItem;
import com.sba301.cinemaai.entity.BookingSeat;
import com.sba301.cinemaai.entity.BookingTicket;
import com.sba301.cinemaai.entity.FoodCombo;
import com.sba301.cinemaai.entity.FoodItem;
import com.sba301.cinemaai.entity.Payment;
import com.sba301.cinemaai.entity.Seat;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.BookingStatus;
import com.sba301.cinemaai.enums.FoodItemStatus;
import com.sba301.cinemaai.enums.PaymentStatus;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.enums.SeatRuntimeStatus;
import com.sba301.cinemaai.enums.SeatStatus;
import com.sba301.cinemaai.enums.ShowtimeStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.ConflictException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.mapper.BookingMapper;
import com.sba301.cinemaai.repository.BookingFoodItemRepository;
import com.sba301.cinemaai.repository.BookingRepository;
import com.sba301.cinemaai.repository.BookingSeatRepository;
import com.sba301.cinemaai.repository.BookingTicketRepository;
import com.sba301.cinemaai.repository.PaymentRepository;
import com.sba301.cinemaai.repository.ReviewRepository;
import com.sba301.cinemaai.repository.SeatRepository;
import com.sba301.cinemaai.repository.ShowtimeRepository;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.BookingService;
import com.sba301.cinemaai.service.FoodService;
import com.sba301.cinemaai.service.LoyaltyPointService;
import com.sba301.cinemaai.service.QrTicketService;
import com.sba301.cinemaai.service.RefundService;
import com.sba301.cinemaai.service.TicketPricingService;
import com.sba301.cinemaai.service.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final int HOLD_MINUTES = 3;
    private static final int CHECK_IN_LEAD_MINUTES = 30;
    private static final List<BookingStatus> ACTIVE_CHECKOUT_STATUSES = List.of(
            BookingStatus.HOLDING,
            BookingStatus.PENDING_PAYMENT
    );
    private static final List<SeatRuntimeStatus> BLOCKING_SEAT_STATUSES = List.of(
            SeatRuntimeStatus.HOLDING,
            SeatRuntimeStatus.BOOKED,
            SeatRuntimeStatus.CHECKED_IN
    );
    private static final List<FoodItemStatus> SELLABLE_FOOD_STATUSES = List.of(
            FoodItemStatus.ACTIVE,
            FoodItemStatus.LOW_STOCK
    );

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingTicketRepository bookingTicketRepository;
    private final BookingFoodItemRepository bookingFoodItemRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final UserService userService;
    private final FoodService foodService;
    private final TicketPricingService ticketPricingService;
    private final QrTicketService qrTicketService;
    private final BookingMapper bookingMapper;
    private final LoyaltyPointService loyaltyPointService;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final AuditLogService auditLogService;
    private final RefundService refundService;

    @Transactional
    public BookingResponse holdSeats(String email, HoldSeatsRequest request) {
        releaseExpiredHolds();
        User user = userService.getByEmail(email);
        // Serialize holds for one showtime so two tabs cannot create concurrent
        // active checkouts for the same customer and screening.
        Showtime showtime = showtimeRepository.findByIdForUpdate(request.showtimeId())
                .orElseThrow(() -> new NotFoundException("Showtime not found"));
        if (showtime.getStatus() != ShowtimeStatus.OPEN) {
            throw new BadRequestException("Showtime is not open for booking");
        }
        if (bookingRepository.existsByUserAndShowtimeAndStatusIn(user, showtime, ACTIVE_CHECKOUT_STATUSES)) {
            throw new ConflictException(
                    "You already have an active booking for this showtime. Continue payment or cancel it before selecting new seats"
            );
        }

        List<Long> requestedSeatIds = request.seatIds().stream().distinct().toList();
        List<Seat> requestedSeats = requestedSeatIds.stream().map(this::findSeat).toList();
        validateCoupleSeatPairs(requestedSeats);

        Booking booking = bookingRepository.save(new Booking(newBookingCode(), user, showtime,
                LocalDateTime.now().plusMinutes(HOLD_MINUTES)));
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Seat seat : requestedSeats) {
            validateSeatForShowtime(showtime, seat);
            BigDecimal unitPrice = showtime.getPriceForSeatType(seat.getSeatType());
            BookingSeat bookingSeat = bookingSeatRepository.save(new BookingSeat(booking, showtime, seat, unitPrice));
            subtotal = subtotal.add(bookingSeat.getUnitPrice());
        }

        if (request.tickets() != null && !request.tickets().isEmpty()) {
            subtotal = applyTicketSelections(booking, request.comboId(), request.holiday(), request.tickets());
        }
        if (request.foods() != null) {
            for (BookingFoodRequest foodRequest : request.foods()) {
                BookingFoodItem item = createFoodItem(booking, foodRequest);
                bookingFoodItemRepository.save(item);
                subtotal = subtotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }

        BigDecimal discountAmount = applyLoyaltyDiscount(user, booking, request.loyaltyPointsToRedeem(), subtotal);
        booking.setSubtotal(subtotal);
        booking.setDiscountAmount(discountAmount);
        booking.setTotalAmount(subtotal.subtract(discountAmount));
        bookingRepository.saveAndFlush(booking);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse createBooking(String email, CreateBookingRequest request) {
        releaseExpiredHolds();
        User user = userService.getByEmail(email);
        Booking booking = findBooking(request.holdBookingId());
        validateOwner(booking, user);
        if (booking.getStatus() != BookingStatus.HOLDING) {
            throw new BadRequestException("Booking is not in holding status");
        }
        if (booking.getHoldExpiresAt() != null && booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            expireBooking(booking);
            throw new BadRequestException("Seat hold has expired");
        }

        BigDecimal subtotal = booking.getSubtotal();
        if (request.tickets() != null && !request.tickets().isEmpty()) {
            subtotal = applyTicketSelections(booking, request.comboId(), request.holiday(), request.tickets());
        }
        if (request.foods() != null) {
            for (BookingFoodRequest foodRequest : request.foods()) {
                BookingFoodItem item = createFoodItem(booking, foodRequest);
                bookingFoodItemRepository.save(item);
                subtotal = subtotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }

        bookingSeatRepository.findByBooking(booking)
                .forEach(seat -> changeBookingSeatStatus(seat, SeatRuntimeStatus.BOOKED));
        BigDecimal discount = booking.getDiscountAmount();
        BigDecimal loyaltyDiscount = request.loyaltyPointsToRedeem() != null && request.loyaltyPointsToRedeem() > 0 && discount.signum() == 0
                ? applyLoyaltyDiscount(user, booking, request.loyaltyPointsToRedeem(), subtotal)
                : discount;
        booking.setSubtotal(subtotal);
        booking.setDiscountAmount(loyaltyDiscount);
        booking.setTotalAmount(subtotal.subtract(loyaltyDiscount));
        bookingRepository.saveAndFlush(booking);
        markPendingPayment(booking);
        return toResponse(booking);
    }

    /**
     * Cho phép quay lại bước bắp nước từ màn thanh toán mà KHÔNG hủy hold ghế:
     * chỉ thay danh sách bắp nước + điểm loyalty rồi tính lại tổng, đồng thời gia hạn hold.
     */
    @Transactional
    public BookingResponse updateHoldingItems(
            String email,
            Long bookingId,
            com.sba301.cinemaai.dto.request.booking.UpdateHoldingBookingRequest request
    ) {
        User user = userService.getByEmail(email);
        Booking booking = findBooking(bookingId);
        validateOwner(booking, user);
        if (booking.getStatus() != BookingStatus.HOLDING && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Only holding bookings can be updated");
        }
        if (booking.getHoldExpiresAt() != null && booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            expireBooking(booking);
            throw new BadRequestException("Seat hold has expired");
        }

        loyaltyPointService.restoreRedeemedPointsFromBooking(user, booking);
        booking.setLoyaltyPointsRedeemed(0);
        booking.setDiscountAmount(BigDecimal.ZERO);

        bookingFoodItemRepository.findByBooking(booking)
                .stream()
                .filter(item -> item.getFoodOrder() == null)
                .forEach(bookingFoodItemRepository::delete);

        List<BookingTicket> ticketLines = bookingTicketRepository.findByBooking(booking);
        BigDecimal subtotal = ticketLines.isEmpty()
                ? bookingSeatRepository.findByBooking(booking).stream()
                        .map(BookingSeat::getUnitPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : ticketLines.stream()
                        .map(BookingTicket::getLineTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (request.foods() != null) {
            for (BookingFoodRequest foodRequest : request.foods()) {
                BookingFoodItem item = createFoodItem(booking, foodRequest);
                bookingFoodItemRepository.save(item);
                subtotal = subtotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }

        BigDecimal discountAmount = applyLoyaltyDiscount(user, booking, request.loyaltyPointsToRedeem(), subtotal);
        booking.setSubtotal(subtotal);
        booking.setDiscountAmount(discountAmount);
        booking.setTotalAmount(subtotal.subtract(discountAmount));
        booking.setHoldExpiresAt(LocalDateTime.now().plusMinutes(HOLD_MINUTES));
        bookingRepository.saveAndFlush(booking);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getMyBookings(String email, int page, int size) {
        User user = userService.getByEmail(email);
        Page<Booking> bookings = bookingRepository.findByUser(user, bookingPageable(page, size));
        return PageResponse.from(new PageImpl<>(
                toResponses(bookings.getContent()), bookings.getPageable(), bookings.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getAdminBookings(BookingStatus status, int page, int size) {
        Pageable pageable = bookingPageable(page, size);
        Page<Booking> bookings = status == null
                ? bookingRepository.findAllWithDetails(pageable)
                : bookingRepository.findByStatus(status, pageable);
        return PageResponse.from(new PageImpl<>(
                toResponses(bookings.getContent()), bookings.getPageable(), bookings.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public BookingResponse getMyBooking(String email, Long bookingId) {
        User user = userService.getByEmail(email);
        Booking booking = findBooking(bookingId);
        validateOwner(booking, user);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getAdminBooking(Long bookingId) {
        return toResponse(findBooking(bookingId));
    }

    @Transactional
    public BookingResponse cancel(String email, Long bookingId) {
        User user = userService.getByEmail(email);
        Booking booking = findBooking(bookingId);
        validateOwner(booking, user);
        return cancelBooking(booking);
    }

    @Transactional
    public BookingResponse cancelAdmin(Long bookingId, String reason) {
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() == BookingStatus.USED) {
            throw new BadRequestException("Checked-in booking cannot be cancelled");
        }
        // Vé đã thanh toán: hoàn tiền về CineWallet (ghi audit REFUND bên trong)
        if (booking.getStatus() == BookingStatus.PAID) {
            refundService.refundPaidBooking(booking, reason);
            return toResponse(booking);
        }
        // Vé chưa thanh toán: hủy như cũ, không phát sinh hoàn tiền
        BookingResponse response = cancelBooking(booking);
        auditLogService.record(com.sba301.cinemaai.enums.AuditActionType.DELETE, "BOOKING",
                booking.getId(), booking.getBookingCode());
        return response;
    }

    private BookingResponse cancelBooking(Booking booking) {
        if (booking.getStatus() == BookingStatus.USED) {
            throw new BadRequestException("Checked-in booking cannot be cancelled");
        }
        releaseSeats(booking);
        cancel(booking);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse checkIn(String qrCode) {
        QrTicketService.QrPayload payload;
        try {
            payload = qrTicketService.parse(qrCode);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(exception.getMessage());
        }

        if (payload.type() == QrTicketService.QrPayloadType.SEAT) {
            BookingSeat bookingSeat = bookingSeatRepository.findByTicketCode(payload.code())
                    .orElseThrow(() -> new NotFoundException("Seat ticket not found"));
            Booking booking = bookingSeat.getBooking();
            checkInSeat(booking, bookingSeat);
            finishCheckInIfComplete(booking);
            auditLogService.record(com.sba301.cinemaai.enums.AuditActionType.CHECK_IN, "BOOKING",
                    booking.getId(), "Seat " + payload.code());
            return toResponse(booking);
        }

        Booking booking = bookingRepository.findByBookingCode(payload.code())
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        checkInRemainingSeats(booking);
        auditLogService.record(com.sba301.cinemaai.enums.AuditActionType.CHECK_IN, "BOOKING",
                booking.getId(), booking.getBookingCode() + " (all seats)");
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse checkInSeats(String bookingCode, List<String> ticketCodes) {
        if (bookingCode == null || bookingCode.isBlank()) {
            throw new BadRequestException("Booking code is required");
        }
        if (ticketCodes == null || ticketCodes.isEmpty()) {
            throw new BadRequestException("At least one ticket code is required");
        }
        Booking booking = bookingRepository.findByBookingCode(bookingCode.trim())
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        Map<String, BookingSeat> seatsByTicketCode = bookingSeatRepository.findByBooking(booking)
                .stream()
                .filter(bookingSeat -> bookingSeat.getTicketCode() != null)
                .collect(Collectors.toMap(BookingSeat::getTicketCode, Function.identity()));
        for (String ticketCode : ticketCodes) {
            BookingSeat bookingSeat = seatsByTicketCode.get(ticketCode == null ? null : ticketCode.trim());
            if (bookingSeat == null) {
                throw new NotFoundException("Seat ticket not found in booking: " + ticketCode);
            }
            checkInSeat(booking, bookingSeat);
        }
        finishCheckInIfComplete(booking);
        auditLogService.record(com.sba301.cinemaai.enums.AuditActionType.CHECK_IN, "BOOKING",
                booking.getId(), booking.getBookingCode() + " seats: " + String.join(", ", ticketCodes));
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse checkInAdmin(Long bookingId, String qrCode) {
        Booking booking = findBooking(bookingId);
        if (qrCode != null && !qrCode.isBlank()) {
            QrTicketService.QrPayload payload;
            try {
                payload = qrTicketService.parse(qrCode);
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException(exception.getMessage());
            }
            boolean matches = payload.type() == QrTicketService.QrPayloadType.BOOKING
                    ? booking.getBookingCode().equals(payload.code())
                    : payload.code().startsWith(booking.getBookingCode() + "-");
            if (!matches) {
                throw new BadRequestException("QR code does not match booking");
            }
        }
        checkInRemainingSeats(booking);
        return toResponse(booking);
    }

    private void checkInRemainingSeats(Booking booking) {
        ensureSeatTicketCodes(booking);
        List<BookingSeat> remaining = bookingSeatRepository.findByBooking(booking)
                .stream()
                .filter(bookingSeat -> bookingSeat.getStatus() != SeatRuntimeStatus.CHECKED_IN)
                .toList();
        if (remaining.isEmpty()) {
            throw new BadRequestException("All seats of this booking are already checked in");
        }
        remaining.forEach(bookingSeat -> checkInSeat(booking, bookingSeat));
        finishCheckInIfComplete(booking);
    }

    private void checkInSeat(Booking booking, BookingSeat bookingSeat) {
        requireStatus(booking, BookingStatus.PAID, "Only paid booking can be checked in");
        if (bookingSeat.getStatus() == SeatRuntimeStatus.CHECKED_IN) {
            throw new BadRequestException("Seat "
                    + bookingSeat.getSeat().getRowLabel() + bookingSeat.getSeat().getSeatNumber()
                    + " is already checked in");
        }
        if (bookingSeat.getStatus() != SeatRuntimeStatus.BOOKED) {
            throw new BadRequestException("Seat is not in booked status");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInOpenAt = booking.getShowtime().getStartTime().minusMinutes(CHECK_IN_LEAD_MINUTES);
        if (now.isBefore(checkInOpenAt)) {
            throw new BadRequestException("Check-in opens 30 minutes before showtime");
        }
        if (booking.getShowtime().getEndTime() != null && now.isAfter(booking.getShowtime().getEndTime())) {
            throw new BadRequestException("Showtime has already ended");
        }
        bookingSeat.setStatus(SeatRuntimeStatus.CHECKED_IN);
        bookingSeat.setCheckedInAt(now);
    }

    private void finishCheckInIfComplete(Booking booking) {
        boolean allCheckedIn = bookingSeatRepository.findByBooking(booking)
                .stream()
                .allMatch(bookingSeat -> bookingSeat.getStatus() == SeatRuntimeStatus.CHECKED_IN);
        if (allCheckedIn && booking.getStatus() == BookingStatus.PAID) {
            booking.setCheckedInAt(LocalDateTime.now());
            booking.setStatus(BookingStatus.USED);
        }
    }

    private void ensureSeatTicketCodes(Booking booking) {
        if (booking.getStatus() != BookingStatus.PAID && booking.getStatus() != BookingStatus.USED) {
            return;
        }
        bookingSeatRepository.findByBooking(booking)
                .stream()
                .filter(bookingSeat -> bookingSeat.getTicketCode() == null)
                .forEach(bookingSeat -> {
                    bookingSeat.setTicketCode(qrTicketService.generateTicketCode(bookingSeat));
                    bookingSeat.setQrCode(qrTicketService.generateSeatQr(bookingSeat));
                });
    }

    @Transactional
    public int releaseExpiredHolds() {
        List<Booking> expiredBookings = bookingRepository.findByStatusAndHoldExpiresAtBefore(
                BookingStatus.HOLDING,
                LocalDateTime.now()
        );
        expiredBookings.addAll(bookingRepository.findByStatusAndHoldExpiresAtBefore(
                BookingStatus.PENDING_PAYMENT,
                LocalDateTime.now()
        ));
        expiredBookings.forEach(this::expireBooking);
        return expiredBookings.size();
    }

    @Transactional
    public BookingResponse lookupForCheckIn(String bookingCode, String qrCode) {
        Booking booking = null;
        String resolvedCode = bookingCode;
        if ((resolvedCode == null || resolvedCode.isBlank()) && qrCode != null && !qrCode.isBlank()) {
            QrTicketService.QrPayload payload;
            try {
                payload = qrTicketService.parse(qrCode);
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException(exception.getMessage());
            }
            if (payload.type() == QrTicketService.QrPayloadType.SEAT) {
                booking = bookingSeatRepository.findByTicketCode(payload.code())
                        .orElseThrow(() -> new NotFoundException("Seat ticket not found"))
                        .getBooking();
            } else {
                resolvedCode = payload.code();
            }
        }
        if (booking == null) {
            if (resolvedCode == null || resolvedCode.isBlank()) {
                throw new BadRequestException("Booking code or QR code is required");
            }
            String trimmedCode = resolvedCode.trim();
            booking = bookingRepository.findByBookingCode(trimmedCode)
                    .or(() -> bookingSeatRepository.findByTicketCode(trimmedCode).map(BookingSeat::getBooking))
                    .orElseThrow(() -> new NotFoundException("Booking not found"));
        }
        ensureSeatTicketCodes(booking);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getRecentStaffCheckInBookings(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return toResponses(bookingRepository.findRecentForCheckIn(
                List.of(BookingStatus.PAID, BookingStatus.USED),
                pageable
        ));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getStaffBookingsByShowtime(Long showtimeId) {
        Showtime showtime = findShowtime(showtimeId);
        return toResponses(bookingRepository.findByShowtime(showtime));
    }

    private BigDecimal applyTicketSelections(
            Booking booking,
            Long comboId,
            boolean holiday,
            List<TicketSelectionRequest> tickets
    ) {
        validateTicketSelectionsMatchHeldSeats(booking, tickets);
        TicketPriceValidationResponse validation = ticketPricingService.validatePrice(new TicketPriceValidationRequest(
                booking.getShowtime().getId(),
                comboId,
                holiday,
                tickets
        ));
        if (!validation.eligible()) {
            throw new BadRequestException("Ticket selection is not eligible for this movie/showtime");
        }
        bookingTicketRepository.findByBooking(booking).forEach(bookingTicketRepository::delete);
        Map<Long, BookingSeat> seatsById = bookingSeatRepository.findByBooking(booking)
                .stream()
                .collect(Collectors.toMap(
                        bookingSeat -> bookingSeat.getSeat().getId(),
                        Function.identity()
                ));
        for (int i = 0; i < validation.tickets().size(); i++) {
            TicketLinePriceResponse ticket = validation.tickets().get(i);
            bookingTicketRepository.save(new BookingTicket(
                    booking,
                    ticket.ticketType(),
                    ticket.viewerAge(),
                    ticket.quantity(),
                    ticket.unitPrice(),
                    ticket.lineTotal()
            ));
            Long seatId = tickets.get(i).seatId();
            if (seatId != null && ticket.quantity() == 1) {
                BookingSeat bookingSeat = seatsById.get(seatId);
                if (bookingSeat != null) {
                    changeBookingSeatUnitPrice(bookingSeat, ticket.unitPrice());
                    bookingSeat.setTicketType(ticket.ticketType());
                }
            }
        }
        return validation.finalAmount();
    }

    private void validateTicketSelectionsMatchHeldSeats(
            Booking booking,
            List<TicketSelectionRequest> tickets
    ) {
        List<BookingSeat> heldSeats = bookingSeatRepository.findByBooking(booking);
        int ticketQuantity = tickets.stream().mapToInt(ticket -> ticket.quantity()).sum();
        if (ticketQuantity != heldSeats.size()) {
            throw new BadRequestException("Ticket quantity must match held seat quantity");
        }

        boolean allTicketsHaveSeatId = tickets.stream().allMatch(ticket -> ticket.seatId() != null);
        if (allTicketsHaveSeatId) {
            Set<Long> heldSeatIds = heldSeats.stream()
                    .map(bookingSeat -> bookingSeat.getSeat().getId())
                    .collect(Collectors.toSet());
            Set<Long> ticketSeatIds = new HashSet<>();
            for (TicketSelectionRequest ticket : tickets) {
                if (ticket.quantity() != 1) {
                    throw new BadRequestException("Ticket quantity must be 1 when seatId is provided");
                }
                if (!ticketSeatIds.add(ticket.seatId())) {
                    throw new BadRequestException("Duplicate ticket seatId: " + ticket.seatId());
                }
            }
            if (!heldSeatIds.equals(ticketSeatIds)) {
                throw new BadRequestException("Ticket seat ids must match held seats");
            }
            return;
        }

        Map<SeatType, Integer> heldBySeatType = new EnumMap<>(SeatType.class);
        for (BookingSeat bookingSeat : heldSeats) {
            heldBySeatType.merge(normalizeSeatType(bookingSeat.getSeat().getSeatType()), 1, Integer::sum);
        }

        Map<SeatType, Integer> ticketsBySeatType = new EnumMap<>(SeatType.class);
        for (TicketSelectionRequest ticket : tickets) {
            ticketsBySeatType.merge(normalizeSeatType(ticket.seatType()), ticket.quantity(), Integer::sum);
        }

        if (!heldBySeatType.equals(ticketsBySeatType)) {
            throw new BadRequestException("Ticket seat types must match held seat types");
        }
    }

    private SeatType normalizeSeatType(SeatType seatType) {
        return seatType == SeatType.NORMAL ? SeatType.STANDARD : seatType;
    }

    private BookingFoodItem createFoodItem(Booking booking, BookingFoodRequest request) {
        if ((request.foodItemId() == null && request.foodComboId() == null)
                || (request.foodItemId() != null && request.foodComboId() != null)) {
            throw new BadRequestException("Choose exactly one food item or combo");
        }
        if (request.quantity() <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }
        if (request.foodItemId() != null) {
            FoodItem foodItem = foodService.findItem(request.foodItemId());
            if (!SELLABLE_FOOD_STATUSES.contains(foodItem.getStatus())) {
                throw new BadRequestException("Food item is not available");
            }
            return new BookingFoodItem(booking, foodItem, null, request.quantity(), foodItem.getPrice());
        }
        FoodCombo foodCombo = foodService.findCombo(request.foodComboId());
        if (!SELLABLE_FOOD_STATUSES.contains(foodCombo.getStatus())) {
            throw new BadRequestException("Food combo is not available");
        }
        return new BookingFoodItem(booking, null, foodCombo, request.quantity(), foodCombo.getPrice());
    }

    private void validateSeatForShowtime(Showtime showtime, Seat seat) {
        if (!seat.getRoom().getId().equals(showtime.getRoom().getId())) {
            throw new BadRequestException("Seat does not belong to showtime room");
        }
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new BadRequestException("Seat is not available");
        }
        boolean blocked = bookingSeatRepository
                .findByShowtimeAndSeatAndStatusIn(showtime, seat, BLOCKING_SEAT_STATUSES)
                .stream()
                .anyMatch(this::isBlockingSeat);
        if (blocked) {
            throw new ConflictException("Seat is already held or booked");
        }
    }

    private void validateCoupleSeatPairs(List<Seat> seats) {
        Set<Long> selectedSeatIds = seats.stream()
                .map(Seat::getId)
                .collect(Collectors.toSet());
        for (Seat seat : seats) {
            if (seat.getSeatType() != SeatType.COUPLE) {
                continue;
            }
            Seat partner = findCouplePartner(seat);
            if (partner.getSeatType() != SeatType.COUPLE || !selectedSeatIds.contains(partner.getId())) {
                throw new BadRequestException("Couple seats must be selected as a pair");
            }
        }
    }

    private Seat findCouplePartner(Seat seat) {
        List<Seat> rowSeats = seatRepository.findByRoom(seat.getRoom())
                .stream()
                .filter(candidate -> candidate.getSeatRow().getId().equals(seat.getSeatRow().getId()))
                .sorted((a, b) -> Integer.compare(a.getDisplayColumn(), b.getDisplayColumn()))
                .toList();
        int seatIndex = rowSeats.stream()
                .map(Seat::getId)
                .toList()
                .indexOf(seat.getId());
        if (seatIndex < 0 || rowSeats.size() % 2 != 0) {
            throw new BadRequestException("Couple seats must be selected as a pair");
        }
        int partnerIndex = seatIndex % 2 == 0 ? seatIndex + 1 : seatIndex - 1;
        if (partnerIndex < 0 || partnerIndex >= rowSeats.size()) {
            throw new BadRequestException("Couple seats must be selected as a pair");
        }
        return rowSeats.get(partnerIndex);
    }

    private boolean isBlockingSeat(BookingSeat bookingSeat) {
        Booking booking = bookingSeat.getBooking();
        if (bookingSeat.getStatus() == SeatRuntimeStatus.HOLDING && booking.getStatus() == BookingStatus.HOLDING) {
            return booking.getHoldExpiresAt() == null || booking.getHoldExpiresAt().isAfter(LocalDateTime.now());
        }
        return bookingSeat.getStatus() == SeatRuntimeStatus.BOOKED
                || bookingSeat.getStatus() == SeatRuntimeStatus.CHECKED_IN;
    }

    private void expireBooking(Booking booking) {
        releaseSeats(booking);
        loyaltyPointService.restoreRedeemedPointsFromBooking(booking.getUser(), booking);
        booking.setStatus(BookingStatus.EXPIRED);
    }

    private void releaseSeats(Booking booking) {
        bookingSeatRepository.findByBooking(booking)
                .forEach(seat -> changeBookingSeatStatus(seat, SeatRuntimeStatus.RELEASED));
    }

    private void markPendingPayment(Booking booking) {
        requireStatus(booking, BookingStatus.HOLDING, "Only holding booking can move to pending payment");
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
    }

    private void cancel(Booking booking) {
        if (booking.getStatus() == BookingStatus.PAID) {
            loyaltyPointService.revokePointsFromBooking(booking.getUser(), booking);
        }
        loyaltyPointService.restoreRedeemedPointsFromBooking(booking.getUser(), booking);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setStatus(BookingStatus.CANCELLED);
    }

    private BigDecimal applyLoyaltyDiscount(User user, Booking booking, Integer pointsToRedeem, BigDecimal subtotal) {
        int requestedPoints = pointsToRedeem == null ? 0 : pointsToRedeem;
        if (requestedPoints <= 0 || subtotal == null || subtotal.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        int maxRedeemablePoints = loyaltyPointService.getMaxRedeemablePointsForAmount(subtotal);
        int points = Math.min(requestedPoints, maxRedeemablePoints);
        int redeemed = loyaltyPointService.redeemPointsForBooking(user, booking, points);
        booking.setLoyaltyPointsRedeemed(points);
        return BigDecimal.valueOf(redeemed);
    }

    private void changeBookingSeatStatus(BookingSeat bookingSeat, SeatRuntimeStatus status) {
        if (status == null) {
            throw new BadRequestException("Seat runtime status is required");
        }
        bookingSeat.setStatus(status);
    }

    private void changeBookingSeatUnitPrice(BookingSeat bookingSeat, BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new BadRequestException("Seat unit price must not be null or negative");
        }
        bookingSeat.setUnitPrice(unitPrice);
    }

    private void requireStatus(Booking booking, BookingStatus expectedStatus, String message) {
        if (booking.getStatus() != expectedStatus) {
            throw new BadRequestException(message);
        }
    }

    private BookingResponse toResponse(Booking booking) {
        return toResponses(List.of(booking)).get(0);
    }

    /**
     * Batch mapping: nạp seats/tickets/foods/payment cho CẢ danh sách bằng 4 query IN
     * thay vì 4 query mỗi booking — DB remote nên số câu SQL quyết định thời gian phản hồi.
     */
    private List<BookingResponse> toResponses(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return List.of();
        }
        Map<Long, List<BookingSeat>> seatsByBookingId = bookingSeatRepository.findByBookingIn(bookings)
                .stream()
                .collect(Collectors.groupingBy(bookingSeat -> bookingSeat.getBooking().getId()));
        Map<Long, List<BookingTicket>> ticketsByBookingId = bookingTicketRepository.findByBookingIn(bookings)
                .stream()
                .collect(Collectors.groupingBy(ticket -> ticket.getBooking().getId()));
        Map<Long, List<BookingFoodItem>> foodsByBookingId = bookingFoodItemRepository.findByBookingIn(bookings)
                .stream()
                .filter(item -> item.getFoodOrder() == null
                        || item.getFoodOrder().getStatus() == com.sba301.cinemaai.enums.FoodOrderStatus.PAID)
                .collect(Collectors.groupingBy(item -> item.getBooking().getId()));
        Map<Long, String> paymentAccountsByBookingId = resolvePaymentAccounts(bookings);
        return bookings.stream()
                .map(booking -> bookingMapper.toResponse(
                        booking,
                        seatsByBookingId.getOrDefault(booking.getId(), List.of()),
                        ticketsByBookingId.getOrDefault(booking.getId(), List.of()),
                        foodsByBookingId.getOrDefault(booking.getId(), List.of()),
                        paymentAccountsByBookingId.get(booking.getId())
                ))
                .toList();
    }

    private Map<Long, String> resolvePaymentAccounts(List<Booking> bookings) {
        List<Long> bookingIds = bookings.stream()
                .filter(booking -> booking.getStatus() != BookingStatus.REFUNDED)
                .map(Booking::getId)
                .toList();
        if (bookingIds.isEmpty()) {
            return Map.of();
        }
        // ORDER BY id DESC toàn cục → payment đầu tiên gặp của mỗi booking là bản mới nhất;
        // Set-guard (thay vì putIfAbsent) để label null của payment mới nhất vẫn thắng.
        Map<Long, String> accounts = new HashMap<>();
        Set<Long> resolved = new HashSet<>();
        for (Payment payment : paymentRepository
                .findByBookingIdInAndStatusAndFoodOrderIsNullOrderByIdDesc(bookingIds, PaymentStatus.SUCCESS)) {
            Long bookingId = payment.getBooking().getId();
            if (resolved.add(bookingId)) {
                accounts.put(bookingId, payment.getPaymentAccountLabel());
            }
        }
        return accounts;
    }

    private void validateOwner(Booking booking, User user) {
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Booking not found");
        }
    }

    private Showtime findShowtime(Long id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Showtime not found"));
    }

    private Seat findSeat(Long id) {
        return seatRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Seat not found"));
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    private Pageable bookingPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
    }

    private String newBookingCode() {
        return "BK" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
