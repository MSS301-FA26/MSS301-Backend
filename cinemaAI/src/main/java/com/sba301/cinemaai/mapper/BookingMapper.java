package com.sba301.cinemaai.mapper;

import com.sba301.cinemaai.dto.response.booking.BookingFoodResponse;
import com.sba301.cinemaai.dto.response.booking.BookingResponse;
import com.sba301.cinemaai.dto.response.booking.BookingSeatResponse;
import com.sba301.cinemaai.dto.response.booking.BookingTicketResponse;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.BookingFoodItem;
import com.sba301.cinemaai.entity.BookingSeat;
import com.sba301.cinemaai.entity.BookingTicket;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(
            Booking booking,
            List<BookingSeat> seats,
            List<BookingTicket> tickets,
            List<BookingFoodItem> foods,
            String paymentAccount
    ) {
        boolean hideSensitiveOrderInfo = booking.getStatus() == com.sba301.cinemaai.enums.BookingStatus.REFUNDED;
        com.sba301.cinemaai.entity.UserProfile profile = booking.getUser().getProfile();
        return new BookingResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getUser().getId(),
                booking.getShowtime().getId(),
                booking.getShowtime().getMovie().getId(),
                booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getMovie().getPosterUrl(),
                booking.getShowtime().getRoom().getName(),
                booking.getShowtime().getRoom().getCinema().getName(),
                booking.getShowtime().getStartTime(),
                booking.getStatus(),
                booking.getSubtotal(),
                booking.getDiscountAmount(),
                booking.getLoyaltyPointsRedeemed(),
                booking.getTotalAmount(),
                booking.getHoldExpiresAt(),
                booking.getPaidAt(),
                booking.getCancelledAt(),
                booking.getCheckedInAt(),
                booking.getRefundRequestedAt(),
                booking.getRefundedAt(),
                booking.getRefundReason(),
                booking.getRefundMethod(),
                booking.isBulkRefund(),
                booking.getRefundRetryAttempts(),
                booking.getLastRefundAttemptAt(),
                hideSensitiveOrderInfo ? null : booking.getQrCode(),
                hideSensitiveOrderInfo ? null : paymentAccount,
                profile == null ? null : profile.getFullName(),
                profile == null ? null : profile.getPhone(),
                booking.getUser().getEmail(),
                booking.getShowtime().getEndTime(),
                booking.getCreatedAt(),
                seats.stream().map(this::toSeatResponse).toList(),
                tickets.stream().map(this::toTicketResponse).toList(),
                foods.stream().map(this::toFoodResponse).toList()
        );
    }

    private BookingSeatResponse toSeatResponse(BookingSeat bookingSeat) {
        boolean hideSensitiveOrderInfo =
                bookingSeat.getBooking().getStatus() == com.sba301.cinemaai.enums.BookingStatus.REFUNDED;
        return new BookingSeatResponse(
                bookingSeat.getSeat().getId(),
                bookingSeat.getSeat().getRowLabel(),
                bookingSeat.getSeat().getSeatNumber(),
                bookingSeat.getUnitPrice(),
                bookingSeat.getStatus(),
                hideSensitiveOrderInfo ? null : bookingSeat.getTicketCode(),
                hideSensitiveOrderInfo ? null : bookingSeat.getQrCode(),
                bookingSeat.getTicketType(),
                bookingSeat.getCheckedInAt()
        );
    }

    private BookingFoodResponse toFoodResponse(BookingFoodItem item) {
        String name = item.getFoodItem() != null ? item.getFoodItem().getName() : item.getFoodCombo().getName();
        Long foodItemId = item.getFoodItem() == null ? null : item.getFoodItem().getId();
        Long foodComboId = item.getFoodCombo() == null ? null : item.getFoodCombo().getId();
        return new BookingFoodResponse(
                foodItemId,
                foodComboId,
                name,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity()))
        );
    }

    private BookingTicketResponse toTicketResponse(BookingTicket ticket) {
        return new BookingTicketResponse(
                ticket.getTicketType(),
                ticket.getViewerAge(),
                ticket.getQuantity(),
                ticket.getUnitPrice(),
                ticket.getLineTotal()
        );
    }
}
