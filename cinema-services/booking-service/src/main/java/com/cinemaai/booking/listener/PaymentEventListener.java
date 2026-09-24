package com.cinemaai.booking.listener;

import com.cinemaai.booking.config.RabbitMqConfig;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.entity.ProcessedEvent;
import com.cinemaai.booking.enums.BookingSeatStatus;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.event.PaymentSucceededEvent;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.ProcessedEventRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final BookingRepository bookingRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConfig.PAYMENT_SUCCEEDED_QUEUE)
    @Transactional
    public void onPaymentSucceeded(String payload) {
        PaymentSucceededEvent event;
        try {
            event = objectMapper.readValue(payload, PaymentSucceededEvent.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize PaymentSucceededEvent payload: {}", payload, ex);
            return;
        }

        log.info("Received PaymentSucceededEvent: eventId={}, bookingId={}, amount={}",
                event.eventId(), event.bookingId(), event.amount());

        // 1. Idempotency Check: prevent duplicate processing
        if (processedEventRepository.existsById(event.eventId())) {
            log.warn("Event {} already processed, skipping duplicate message.", event.eventId());
            return;
        }

        if (event.bookingId() != null) {
            Booking booking = bookingRepository.findById(event.bookingId()).orElse(null);
            if (booking == null) {
                log.error("Booking with id {} not found for payment success event", event.bookingId());
                return;
            }

            // 2. Transition Booking status to PAID
            booking.setStatus(BookingStatus.PAID);
            booking.setPaidAt(event.paidAt() != null ? event.paidAt() : LocalDateTime.now());

            // 3. Generate QR code for Booking Check-in
            String bookingQr = "CINEMA:" + booking.getBookingCode() + ":" + booking.getId();
            booking.setQrCode(bookingQr);

            // 4. Update all held seats to BOOKED and generate individual ticket codes & QR
            if (booking.getSeats() != null) {
                for (BookingSeat seat : booking.getSeats()) {
                    seat.setStatus(BookingSeatStatus.BOOKED);
                    String ticketCode = booking.getBookingCode() + "-" + seat.getRowLabel() + seat.getSeatNumber();
                    seat.setTicketCode(ticketCode);
                    seat.setQrCode("TICKET:" + ticketCode);
                }
            }

            bookingRepository.save(booking);
            log.info("Successfully marked Booking {} as PAID with QR code.", booking.getBookingCode());
        }

        // 5. Record processed event for idempotent consumer guarantee
        processedEventRepository.save(new ProcessedEvent(
                event.eventId(),
                "PaymentSucceededEvent",
                LocalDateTime.now()
        ));
    }
}
