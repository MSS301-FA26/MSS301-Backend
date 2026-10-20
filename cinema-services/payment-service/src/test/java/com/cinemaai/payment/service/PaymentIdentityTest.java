package com.cinemaai.payment.service;

import com.cinemaai.payment.client.BookingClient;
import com.cinemaai.payment.entity.Payment;
import com.cinemaai.payment.exception.UnauthorizedException;
import com.cinemaai.payment.mapper.PaymentMapper;
import com.cinemaai.payment.repository.OutboxEventRepository;
import com.cinemaai.payment.repository.PaymentRepository;
import com.cinemaai.payment.service.impl.PaymentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PaymentIdentityTest {
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    private final OutboxPublisherWorker worker = mock(OutboxPublisherWorker.class);
    private final BookingClient booking = mock(BookingClient.class);
    private final PaymentServiceImpl service = new PaymentServiceImpl(payments, outbox, worker, booking,
            mock(LoyaltyService.class), new ObjectMapper().findAndRegisterModules(), mock(PaymentMapper.class));

    @Test void missingIdentityCannotCreateVnpayOrMockPaymentForBookingOrFoodOrder() {
        for (Long id : new Long[]{null, 0L, -1L}) {
            assertThrows(UnauthorizedException.class, () -> service.createVnpayPayment(id, 7L, null, "127.0.0.1"));
            assertThrows(UnauthorizedException.class, () -> service.createVnpayPayment(id, null, 8L, "127.0.0.1"));
            assertThrows(UnauthorizedException.class, () -> service.mockPayment(id, 7L, null));
            assertThrows(UnauthorizedException.class, () -> service.mockPayment(id, null, 8L));
        }
        verifyNoInteractions(payments, outbox, worker, booking);
    }

    @Test void authenticatedUserCreatesPaymentsForOwnedBookingAndFoodOrder() {
        when(booking.getBooking(7L)).thenReturn(new BookingClient.BookingInfo(7L, "BK7", 23L,
                BigDecimal.valueOf(100), "PENDING_PAYMENT", LocalDateTime.now().plusMinutes(2), 0, 1L));
        when(booking.getFoodOrder(8L)).thenReturn(new BookingClient.FoodOrderInfo(8L, "FO8", 23L,
                BigDecimal.valueOf(100), "PENDING"));
        when(payments.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0); payment.setId(42L); return payment;
        });
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0); payment.setId(42L); return payment;
        });
        ReflectionTestUtils.setField(service, "vnpTmnCode", "test");
        ReflectionTestUtils.setField(service, "vnpHashSecret", "test-secret");
        ReflectionTestUtils.setField(service, "vnpPayUrl", "https://example.test/pay");
        ReflectionTestUtils.setField(service, "vnpReturnUrl", "https://example.test/return");

        service.createVnpayPayment(23L, 7L, null, "127.0.0.1");
        service.createVnpayPayment(23L, null, 8L, "127.0.0.1");
        service.mockPayment(23L, 7L, null);
        service.mockPayment(23L, null, 8L);
        var captured = org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(payments, times(2)).saveAndFlush(captured.capture());
        verify(payments, atLeast(4)).save(captured.capture());
        for (Payment payment : captured.getAllValues()) assertEquals(23L, payment.getUserId());
        verify(outbox, times(2)).save(any());
    }
}
