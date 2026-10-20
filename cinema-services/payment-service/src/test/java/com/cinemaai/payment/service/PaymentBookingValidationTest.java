package com.cinemaai.payment.service;

import com.cinemaai.payment.client.BookingClient;
import com.cinemaai.payment.exception.BadRequestException;
import com.cinemaai.payment.service.impl.PaymentServiceImpl;
import com.cinemaai.payment.repository.PaymentRepository;
import com.cinemaai.payment.repository.OutboxEventRepository;
import com.cinemaai.payment.mapper.PaymentMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import java.lang.reflect.Proxy;

class PaymentBookingValidationTest {
    private final BookingClient bookingClient = mock(BookingClient.class);
    private final LoyaltyService loyaltyService = (LoyaltyService) Proxy.newProxyInstance(
            LoyaltyService.class.getClassLoader(), new Class<?>[]{LoyaltyService.class},
            (proxy, method, args) -> method.getReturnType() == int.class ? 0 : null);
    private final PaymentServiceImpl service = new PaymentServiceImpl(mock(PaymentRepository.class),
            mock(OutboxEventRepository.class), mock(OutboxPublisherWorker.class), bookingClient,
            loyaltyService, new ObjectMapper(), mock(PaymentMapper.class));

    private void rejects(Long owner, String status, LocalDateTime expires) {
        when(bookingClient.getBooking(7L)).thenReturn(new BookingClient.BookingInfo(
                7L, "BK7", owner, BigDecimal.valueOf(100000), status, expires, 0, 1L));
        assertThrows(BadRequestException.class, () -> service.createVnpayPayment(1L, 7L, null, "127.0.0.1"));
    }

    @Test void rejectsAnotherUsersBooking() { rejects(2L, "PENDING_PAYMENT", LocalDateTime.now().plusMinutes(2)); }
    @Test void rejectsExpiredHold() { rejects(1L, "PENDING_PAYMENT", LocalDateTime.now().minusSeconds(1)); }
    @Test void rejectsPaidBooking() { rejects(1L, "PAID", LocalDateTime.now().plusMinutes(2)); }
    @Test void rejectsCancelledBooking() { rejects(1L, "CANCELLED", LocalDateTime.now().plusMinutes(2)); }
}
