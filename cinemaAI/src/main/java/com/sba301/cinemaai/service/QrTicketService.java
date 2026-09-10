package com.sba301.cinemaai.service;

import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.BookingSeat;
import com.sba301.cinemaai.entity.FoodOrder;

public interface QrTicketService {

    String generate(Booking booking);

    String generateTicketCode(BookingSeat bookingSeat);

    String generateSeatQr(BookingSeat bookingSeat);

    String generateFoodOrderQr(FoodOrder foodOrder);

    String extractBookingCode(String qrCode);

    QrPayload parse(String qrCode);

    enum QrPayloadType { BOOKING, SEAT, FOOD }

    record QrPayload(QrPayloadType type, String code) {
    }
}
