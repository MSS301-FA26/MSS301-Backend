package com.cinemaai.booking.listener;

import com.cinemaai.booking.config.RabbitMqConfig;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.entity.ProcessedEvent;
import com.cinemaai.booking.enums.BookingSeatStatus;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.event.PaymentSucceededEvent;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.FoodOrderRepository;
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
    private final FoodOrderRepository foodOrderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final com.cinemaai.booking.client.PaymentClient paymentClient;
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

        log.info("Received PaymentSucceededEvent: eventId={}, bookingId={}, foodOrderId={}, amount={}",
                event.eventId(), event.bookingId(), event.foodOrderId(), event.amount());

        // 1. Idempotency Check: prevent duplicate processing
        if (processedEventRepository.existsById(event.eventId())) {
            log.warn("Event {} already processed, skipping duplicate message.", event.eventId());
            return;
        }

        if (event.bookingId() != null) {
            Booking booking = bookingRepository.findByIdForUpdate(event.bookingId()).orElse(null);
            if (booking == null) {
                log.error("Booking with id {} not found for payment success event", event.bookingId());
                return;
            }

            if (booking.getStatus() != BookingStatus.PAID && booking.getLoyaltyPointsRedeemed() > 0) {
                log.error("Payment event {} for booking {} requires manual reconciliation because the booking contains unverified loyalty redemption",
                        event.eventId(), booking.getId());
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            if (booking.getStatus() == BookingStatus.PAID) {
                awardBookingLoyalty(booking);
                processedEventRepository.save(new ProcessedEvent(
                        event.eventId(),
                        "PaymentSucceededEvent",
                        now
                ));
                return;
            }

            boolean expired = booking.getHoldExpiresAt() == null || !booking.getHoldExpiresAt().isAfter(now);
            if (booking.getStatus() == BookingStatus.EXPIRED
                    || (booking.getStatus() == BookingStatus.PENDING_PAYMENT && expired)) {
                log.warn("PaymentSucceededEvent received for expired booking #{}. Tickets will not be issued; reconciliation is required.", booking.getId());
                if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                    booking.setStatus(BookingStatus.EXPIRED);
                    booking.setCancelledAt(now);
                    if (booking.getSeats() != null) {
                        booking.getSeats().stream()
                                .filter(seat -> seat.getStatus() == BookingSeatStatus.HOLDING)
                                .forEach(seat -> seat.setStatus(BookingSeatStatus.RELEASED));
                    }
                    bookingRepository.save(booking);
                }
                processedEventRepository.save(new ProcessedEvent(event.eventId(), "PaymentSucceededEvent", now));
                return;
            }

            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                    || booking.getSeats() == null
                    || booking.getSeats().stream().anyMatch(seat -> seat.getStatus() != BookingSeatStatus.HOLDING)) {
                log.error("Payment event {} cannot fulfil booking {} in state {}; manual reconciliation is required",
                        event.eventId(), booking.getId(), booking.getStatus());
                processedEventRepository.save(new ProcessedEvent(event.eventId(), "PaymentSucceededEvent", now));
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

            awardBookingLoyalty(booking);
        }

        if (event.foodOrderId() != null) {
            foodOrderRepository.findById(event.foodOrderId()).ifPresent(order -> {
                order.setStatus("PAID");
                order.setPaidAt(event.paidAt() != null ? event.paidAt() : LocalDateTime.now());
                foodOrderRepository.save(order);
                log.info("Successfully marked FoodOrder {} as PAID.", order.getFoodOrderCode());
            });
        }

        // 5. Record processed event for idempotent consumer guarantee
        processedEventRepository.save(new ProcessedEvent(
                event.eventId(),
                "PaymentSucceededEvent",
                LocalDateTime.now()
        ));
    }

    private void awardBookingLoyalty(Booking booking) {
        if (booking.getUserId() != null) {
            paymentClient.awardLoyaltyPoints(
                    booking.getUserId(), booking.getId(), booking.getBookingCode(),
                    booking.getTotalAmount(), booking.getLoyaltyPointsRedeemed(), booking.getCinemaId());
        }
    }
}
