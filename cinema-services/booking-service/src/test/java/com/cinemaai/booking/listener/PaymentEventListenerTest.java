package com.cinemaai.booking.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.event.PaymentSucceededEvent;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private PaymentEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PaymentEventListener(bookingRepository, processedEventRepository, objectMapper);
    }

    @Test
    void replayedEventDoesNotUpdateBookingOrGenerateQrAgain() throws Exception {
        when(processedEventRepository.claimEvent("payment-succeeded-10", "PaymentSucceededEvent"))
                .thenReturn(0);

        listener.onPaymentSucceeded(payload());

        verify(bookingRepository, never()).findById(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void claimedEventMarksBookingPaidAndPersistsGeneratedQr() throws Exception {
        Booking booking = Booking.builder()
                .id(99L)
                .bookingCode("BK99")
                .status(BookingStatus.PENDING_PAYMENT)
                .build();
        when(processedEventRepository.claimEvent("payment-succeeded-10", "PaymentSucceededEvent"))
                .thenReturn(1);
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking));

        listener.onPaymentSucceeded(payload());

        org.junit.jupiter.api.Assertions.assertEquals(BookingStatus.PAID, booking.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("CINEMA:BK99:99", booking.getQrCode());
        verify(bookingRepository).save(booking);
    }

    private String payload() throws Exception {
        return objectMapper.writeValueAsString(new PaymentSucceededEvent(
                "payment-succeeded-10",
                10L,
                99L,
                null,
                7L,
                BigDecimal.valueOf(150_000),
                "VNPAY",
                LocalDateTime.of(2026, 10, 6, 10, 0)));
    }
}
