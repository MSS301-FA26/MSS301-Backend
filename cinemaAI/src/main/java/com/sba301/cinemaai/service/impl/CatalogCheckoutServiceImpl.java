package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.booking.BookingFoodRequest;
import com.sba301.cinemaai.dto.request.catalog.CheckoutQuoteRequest;
import com.sba301.cinemaai.dto.request.promotion.ValidateVoucherRequest;
import com.sba301.cinemaai.dto.response.catalog.CheckoutQuoteResponse;
import com.sba301.cinemaai.dto.response.promotion.ValidateVoucherResponse;
import com.sba301.cinemaai.entity.FoodCombo;
import com.sba301.cinemaai.entity.FoodItem;
import com.sba301.cinemaai.entity.Seat;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.FoodItemStatus;
import com.sba301.cinemaai.enums.PromotionTarget;
import com.sba301.cinemaai.enums.SeatStatus;
import com.sba301.cinemaai.enums.ShowtimeStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.SeatRepository;
import com.sba301.cinemaai.repository.ShowtimeRepository;
import com.sba301.cinemaai.service.CatalogCheckoutService;
import com.sba301.cinemaai.service.FoodService;
import com.sba301.cinemaai.service.LoyaltyPointService;
import com.sba301.cinemaai.service.PromotionService;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.service.UserService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.BookingSeat;
import com.sba301.cinemaai.enums.BookingStatus;
import com.sba301.cinemaai.enums.SeatRuntimeStatus;
import com.sba301.cinemaai.repository.BookingSeatRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogCheckoutServiceImpl implements CatalogCheckoutService {

    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final FoodService foodService;
    private final PromotionService promotionService;
    private final LoyaltyPointService loyaltyPointService;
    private final UserService userService;
    private final BookingSeatRepository bookingSeatRepository;

    @Override
    @Transactional(readOnly = true)
    public CheckoutQuoteResponse getCheckoutQuote(String email, CheckoutQuoteRequest request) {
        Showtime showtime = showtimeRepository.findById(request.showtimeId())
                .orElseThrow(() -> new NotFoundException("Showtime not found"));

        if (showtime.getStatus() != ShowtimeStatus.OPEN && showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new BadRequestException("Showtime is not open for booking");
        }

        // Summary objects
        CheckoutQuoteResponse.MovieSummary movieSummary = new CheckoutQuoteResponse.MovieSummary(
                showtime.getMovie().getId(),
                showtime.getMovie().getTitle(),
                showtime.getMovie().getPosterUrl(),
                showtime.getMovie().getAgeRating() != null ? showtime.getMovie().getAgeRating().name() : null,
                showtime.getMovie().getDurationMinutes()
        );

        CheckoutQuoteResponse.CinemaSummary cinemaSummary = new CheckoutQuoteResponse.CinemaSummary(
                showtime.getRoom().getCinema().getId(),
                showtime.getRoom().getCinema().getName(),
                showtime.getRoom().getCinema().getAddress(),
                showtime.getRoom().getName()
        );

        CheckoutQuoteResponse.ShowtimeSummary showtimeSummary = new CheckoutQuoteResponse.ShowtimeSummary(
                showtime.getId(),
                showtime.getStartTime(),
                showtime.getEndTime()
        );

        // Resolve seats and ticket subtotal
        List<CheckoutQuoteResponse.SeatQuoteItem> seatQuoteItems = new ArrayList<>();
        BigDecimal ticketSubtotal = BigDecimal.ZERO;

        if (request.seatIds() != null && !request.seatIds().isEmpty()) {
            List<Seat> seats = seatRepository.findAllById(request.seatIds());
            validateCoupleSeatPairs(seats);
            validateNoOrphanSeats(showtime, seats);
            for (Seat seat : seats) {
                if (!seat.getRoom().getId().equals(showtime.getRoom().getId())) {
                    throw new BadRequestException("Seat " + seat.getRowLabel() + seat.getSeatNumber() + " does not belong to room");
                }
                if (seat.getStatus() != SeatStatus.AVAILABLE) {
                    throw new BadRequestException("Seat " + seat.getRowLabel() + seat.getSeatNumber() + " is not available");
                }
                BigDecimal unitPrice = showtime.getPriceForSeatType(seat.getSeatType());
                String code = seat.getRowLabel() + seat.getSeatNumber();
                seatQuoteItems.add(new CheckoutQuoteResponse.SeatQuoteItem(
                        seat.getId(),
                        code,
                        seat.getRowLabel(),
                        seat.getSeatNumber(),
                        seat.getSeatType(),
                        unitPrice
                ));
                ticketSubtotal = ticketSubtotal.add(unitPrice);
            }
        }

        // Resolve food items and food subtotal
        List<CheckoutQuoteResponse.FoodQuoteItem> foodQuoteItems = new ArrayList<>();
        BigDecimal foodSubtotal = BigDecimal.ZERO;

        if (request.foods() != null && !request.foods().isEmpty()) {
            for (BookingFoodRequest foodReq : request.foods()) {
                if (foodReq.quantity() <= 0) continue;

                if (foodReq.foodItemId() != null) {
                    FoodItem item = foodService.findItem(foodReq.foodItemId());
                    if (item.getStatus() == FoodItemStatus.ACTIVE) {
                        BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(foodReq.quantity()));
                        foodQuoteItems.add(new CheckoutQuoteResponse.FoodQuoteItem(
                                item.getId(),
                                item.getName(),
                                foodReq.quantity(),
                                item.getPrice(),
                                itemTotal
                        ));
                        foodSubtotal = foodSubtotal.add(itemTotal);
                    }
                } else if (foodReq.foodComboId() != null) {
                    FoodCombo combo = foodService.findCombo(foodReq.foodComboId());
                    if (combo.getStatus() == FoodItemStatus.ACTIVE) {
                        BigDecimal comboTotal = combo.getPrice().multiply(BigDecimal.valueOf(foodReq.quantity()));
                        foodQuoteItems.add(new CheckoutQuoteResponse.FoodQuoteItem(
                                combo.getId(),
                                combo.getName(),
                                foodReq.quantity(),
                                combo.getPrice(),
                                comboTotal
                        ));
                        foodSubtotal = foodSubtotal.add(comboTotal);
                    }
                }
            }
        }

        BigDecimal subtotal = ticketSubtotal.add(foodSubtotal);
        BigDecimal discount = BigDecimal.ZERO;
        String voucherMessage = null;

        User user = null;
        if (email != null && !email.isBlank()) {
            try {
                user = userService.getByEmail(email);
            } catch (Exception ignored) {
            }
        }

        // Resolve voucher discount
        if (request.voucherCode() != null && !request.voucherCode().isBlank() && subtotal.signum() > 0) {
            try {
                ValidateVoucherResponse voucherResp = promotionService.validateVoucher(
                        new ValidateVoucherRequest(request.voucherCode().trim(), subtotal, PromotionTarget.ALL),
                        user != null ? user.getId() : null
                );
                if (voucherResp.valid()) {
                    discount = voucherResp.discountAmount() != null ? voucherResp.discountAmount() : BigDecimal.ZERO;
                    voucherMessage = voucherResp.message();
                } else {
                    voucherMessage = voucherResp.message();
                }
            } catch (Exception e) {
                voucherMessage = e.getMessage();
            }
        }

        // Resolve CinePoints (loyalty points) discount
        BigDecimal cinePointsDiscount = BigDecimal.ZERO;
        if (user != null && request.cinePointsToUse() != null && request.cinePointsToUse() > 0 && subtotal.signum() > 0) {
            BigDecimal remainingAfterVoucher = subtotal.subtract(discount);
            if (remainingAfterVoucher.signum() > 0) {
                int maxRedeemablePoints = loyaltyPointService.getMaxRedeemablePointsForAmount(remainingAfterVoucher);
                int points = Math.min(request.cinePointsToUse(), maxRedeemablePoints);
                cinePointsDiscount = BigDecimal.valueOf(points); // 1 point = 1 VND
            }
        }

        BigDecimal fees = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(discount).subtract(cinePointsDiscount).max(BigDecimal.ZERO);

        return new CheckoutQuoteResponse(
                movieSummary,
                cinemaSummary,
                showtimeSummary,
                seatQuoteItems,
                foodQuoteItems,
                ticketSubtotal,
                foodSubtotal,
                subtotal,
                discount,
                cinePointsDiscount,
                fees,
                tax,
                total,
                voucherMessage
        );
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
                throw new BadRequestException("Ghế đôi " + seat.getRowLabel() + seat.getSeatNumber() + " phải được đặt theo cặp");
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
            throw new BadRequestException("Ghế đôi phải được đặt theo cặp");
        }
        int partnerIndex = seatIndex % 2 == 0 ? seatIndex + 1 : seatIndex - 1;
        if (partnerIndex < 0 || partnerIndex >= rowSeats.size()) {
            throw new BadRequestException("Ghế đôi phải được đặt theo cặp");
        }
        return rowSeats.get(partnerIndex);
    }

    private void validateNoOrphanSeats(Showtime showtime, List<Seat> requestedSeats) {
        if (requestedSeats == null || requestedSeats.isEmpty()) {
            return;
        }

        List<Seat> requestedSingles = requestedSeats.stream()
                .filter(s -> s.getSeatType() == SeatType.SINGLE)
                .toList();
        if (requestedSingles.isEmpty()) {
            return;
        }

        Set<Long> requestedSeatIds = requestedSeats.stream()
                .map(Seat::getId)
                .collect(Collectors.toSet());

        Set<Long> occupiedSeatIds = bookingSeatRepository.findByShowtime(showtime).stream()
                .filter(this::isBlockingSeat)
                .map(bs -> bs.getSeat().getId())
                .filter(id -> !requestedSeatIds.contains(id))
                .collect(Collectors.toSet());

        List<Seat> allRoomSeats = seatRepository.findByRoomId(showtime.getRoom().getId());

        Map<String, List<Seat>> requestedByRow = requestedSingles.stream()
                .collect(Collectors.groupingBy(Seat::getRowLabel));

        Map<String, List<Seat>> allSeatsByRow = allRoomSeats.stream()
                .filter(s -> s.getSeatType() == SeatType.SINGLE)
                .collect(Collectors.groupingBy(Seat::getRowLabel));

        for (Map.Entry<String, List<Seat>> entry : requestedByRow.entrySet()) {
            String rowLabel = entry.getKey();
            List<Seat> rowSeats = allSeatsByRow.getOrDefault(rowLabel, List.of()).stream()
                    .sorted(Comparator.comparingInt(Seat::getDisplayColumn))
                    .toList();

            if (rowSeats.isEmpty()) continue;

            List<List<Seat>> sections = new ArrayList<>();
            List<Seat> currentSection = new ArrayList<>();

            for (Seat seat : rowSeats) {
                if (currentSection.isEmpty()) {
                    currentSection.add(seat);
                } else {
                    Seat prev = currentSection.get(currentSection.size() - 1);
                    if (seat.getDisplayColumn() - prev.getDisplayColumn() == 1) {
                        currentSection.add(seat);
                    } else {
                        sections.add(currentSection);
                        currentSection = new ArrayList<>();
                        currentSection.add(seat);
                    }
                }
            }
            if (!currentSection.isEmpty()) {
                sections.add(currentSection);
            }

            for (List<Seat> section : sections) {
                boolean sectionTouched = section.stream()
                        .anyMatch(s -> requestedSeatIds.contains(s.getId()));
                if (!sectionTouched) continue;

                int availableRunLength = 0;
                for (Seat seat : section) {
                    boolean isOccupied = occupiedSeatIds.contains(seat.getId())
                            || requestedSeatIds.contains(seat.getId());
                    if (isOccupied) {
                        if (availableRunLength == 1) {
                            throw new BadRequestException("INVALID_SEAT_GAP: Không thể để trống một ghế đơn lẻ giữa các ghế. Vui lòng chọn vị trí khác.");
                        }
                        availableRunLength = 0;
                    } else {
                        availableRunLength++;
                    }
                }
                if (availableRunLength == 1) {
                    throw new BadRequestException("INVALID_SEAT_GAP: Không thể để trống một ghế đơn lẻ giữa các ghế. Vui lòng chọn vị trí khác.");
                }
            }
        }
    }

    private boolean isBlockingSeat(BookingSeat bookingSeat) {
        Booking booking = bookingSeat.getBooking();
        if (bookingSeat.getStatus() == SeatRuntimeStatus.HOLDING && booking.getStatus() == BookingStatus.HOLDING) {
            return booking.getHoldExpiresAt() == null || booking.getHoldExpiresAt().isAfter(LocalDateTime.now());
        }
        return bookingSeat.getStatus() == SeatRuntimeStatus.BOOKED
                || bookingSeat.getStatus() == SeatRuntimeStatus.CHECKED_IN;
    }
}
