package com.cinemaai.booking.mapper;

import com.cinemaai.booking.dto.response.BookingFoodResponse;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.dto.response.BookingSeatResponse;
import com.cinemaai.booking.dto.response.BookingTicketResponse;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingFoodItem;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.entity.BookingTicket;
import java.util.List;

public class BookingMapper {

    public static BookingResponse toResponse(Booking booking) {
        if (booking == null) return null;

        List<BookingSeatResponse> seatResponses = booking.getSeats() == null ? List.of() :
                booking.getSeats().stream().map(BookingMapper::toSeatResponse).toList();

        List<BookingTicketResponse> ticketResponses = booking.getTickets() == null ? List.of() :
                booking.getTickets().stream().map(BookingMapper::toTicketResponse).toList();

        List<BookingFoodResponse> foodResponses = booking.getFoodItems() == null ? List.of() :
                booking.getFoodItems().stream().map(BookingMapper::toFoodResponse).toList();

        return new BookingResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getUserId(),
                booking.getShowtimeId(),
                booking.getMovieId(),
                booking.getMovieTitleSnapshot(),
                booking.getMovieTitleSnapshot(),
                booking.getMoviePosterSnapshot(),
                booking.getMoviePosterSnapshot(),
                booking.getCinemaNameSnapshot(),
                booking.getCinemaNameSnapshot(),
                booking.getRoomNameSnapshot(),
                booking.getRoomNameSnapshot(),
                booking.getShowtimeStartSnapshot(),
                booking.getShowtimeStartSnapshot(),
                booking.getSubtotal(),
                booking.getDiscountAmount(),
                booking.getLoyaltyPointsRedeemed(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getHoldExpiresAt(),
                booking.getPaidAt(),
                booking.getCheckedInAt(),
                booking.getCancelledAt(),
                booking.getRefundedAt(),
                booking.getQrCode(),
                seatResponses,
                ticketResponses,
                foodResponses,
                booking.getCreatedAt()
        );
    }

    public static BookingSeatResponse toSeatResponse(BookingSeat seat) {
        if (seat == null) return null;
        String seatLabel = seat.getRowLabel() + seat.getSeatNumber();
        return new BookingSeatResponse(
                seat.getId(),
                seat.getSeatId(),
                seat.getShowtimeId(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seatLabel,
                seat.getSeatType(),
                seat.getUnitPrice(),
                seat.getStatus(),
                seat.getTicketCode(),
                seat.getQrCode(),
                seat.getTicketType(),
                seat.getCheckedInAt()
        );
    }

    public static BookingTicketResponse toTicketResponse(BookingTicket ticket) {
        if (ticket == null) return null;
        return new BookingTicketResponse(
                ticket.getId(),
                ticket.getSeatId(),
                ticket.getTicketType(),
                ticket.getViewerAge(),
                ticket.getQuantity(),
                ticket.getUnitPrice(),
                ticket.getLineTotal()
        );
    }

    public static BookingFoodResponse toFoodResponse(BookingFoodItem food) {
        if (food == null) return null;
        return new BookingFoodResponse(
                food.getId(),
                food.getProductId(),
                food.isCombo(),
                food.getProductNameSnapshot(),
                food.getQuantity(),
                food.getUnitPrice(),
                food.getLineTotal()
        );
    }
}
