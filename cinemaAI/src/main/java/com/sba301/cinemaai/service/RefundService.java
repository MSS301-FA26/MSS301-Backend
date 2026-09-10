package com.sba301.cinemaai.service;

import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.Showtime;

public interface RefundService {

    void processShowtimeCancellation(Showtime showtime, String reason);

    /**
     * Hoàn tiền một vé đã thanh toán (PAID) vào CineWallet của khách và đặt trạng thái REFUNDED.
     * Dùng cho luồng admin hủy vé lẻ. Idempotent theo booking (guard REFUND_CREDIT).
     */
    void refundPaidBooking(Booking booking, String reason);
}
