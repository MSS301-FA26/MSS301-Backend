package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.BookingSeat;
import com.sba301.cinemaai.entity.FoodOrder;
import org.springframework.stereotype.Service;
import com.sba301.cinemaai.service.QrTicketService;

@Service
public class QrTicketServiceImpl implements QrTicketService {

    private static final String PREFIX = "CINEAI:";
    private static final String SEAT_MARKER = "SEAT";
    private static final String FOOD_MARKER = "FOOD";

    public String generate(Booking booking) {
        return PREFIX + booking.getBookingCode() + ":" + booking.getUser().getId();
    }

    public String generateTicketCode(BookingSeat bookingSeat) {
        return bookingSeat.getBooking().getBookingCode()
                + "-" + bookingSeat.getSeat().getRowLabel() + bookingSeat.getSeat().getSeatNumber();
    }

    public String generateSeatQr(BookingSeat bookingSeat) {
        String ticketCode = bookingSeat.getTicketCode() != null
                ? bookingSeat.getTicketCode()
                : generateTicketCode(bookingSeat);
        return PREFIX + SEAT_MARKER + ":" + ticketCode + ":" + bookingSeat.getBooking().getUser().getId();
    }

    @Override
    public String generateFoodOrderQr(FoodOrder foodOrder) {
        if (foodOrder.getCustomer() == null) {
            throw new IllegalArgumentException("Food order has no customer");
        }
        return PREFIX + FOOD_MARKER + ":" + foodOrder.getOrderCode() + ":" + foodOrder.getCustomer().getId();
    }

    public String extractBookingCode(String qrCode) {
        QrPayload payload = parse(qrCode);
        if (payload.type() != QrPayloadType.BOOKING) {
            throw new IllegalArgumentException("QR code is not a booking code");
        }
        return payload.code();
    }

    public QrPayload parse(String qrCode) {
        if (qrCode == null || !qrCode.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Invalid QR code");
        }
        String[] parts = qrCode.split(":");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid QR code");
        }
        if (SEAT_MARKER.equals(parts[1])) {
            if (parts.length < 4 || parts[2].isBlank()) {
                throw new IllegalArgumentException("Invalid seat QR code");
            }
            return new QrPayload(QrPayloadType.SEAT, parts[2]);
        }
        if (FOOD_MARKER.equals(parts[1])) {
            if (parts.length < 4 || parts[2].isBlank()) {
                throw new IllegalArgumentException("Invalid food-order QR code");
            }
            return new QrPayload(QrPayloadType.FOOD, parts[2]);
        }
        return new QrPayload(QrPayloadType.BOOKING, parts[1]);
    }
}
