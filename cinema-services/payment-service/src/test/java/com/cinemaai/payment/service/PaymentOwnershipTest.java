package com.cinemaai.payment.service;

import com.cinemaai.payment.client.BookingClient;
import com.cinemaai.payment.exception.NotFoundException;
import com.cinemaai.payment.entity.Payment;
import com.cinemaai.payment.enums.PaymentStatus;
import com.cinemaai.payment.enums.PaymentProvider;
import java.util.Optional;
import com.cinemaai.payment.mapper.PaymentMapper;
import com.cinemaai.payment.repository.*;
import com.cinemaai.payment.service.impl.PaymentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class PaymentOwnershipTest {
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    private final OutboxPublisherWorker worker = mock(OutboxPublisherWorker.class);
    private final BookingClient booking = mock(BookingClient.class);
    private final PaymentServiceImpl service = new PaymentServiceImpl(payments, outbox, worker, booking,
            mock(LoyaltyService.class), new ObjectMapper().findAndRegisterModules(), mock(PaymentMapper.class));

    @Test void foreignPaymentCannotBeReadThroughAnyLookup() {
        Payment other = Payment.builder().id(30L).userId(2L).bookingId(7L).foodOrderId(8L).build();
        when(payments.findById(30L)).thenReturn(Optional.of(other));
        when(payments.findByBookingId(7L)).thenReturn(Optional.of(other));
        when(payments.findByFoodOrderId(8L)).thenReturn(Optional.of(other));
        assertThrows(NotFoundException.class, () -> service.getPaymentById(1L, 30L));
        assertThrows(NotFoundException.class, () -> service.getPaymentByBooking(1L, 7L));
        assertThrows(NotFoundException.class, () -> service.getPaymentByFoodOrder(1L, 8L));
        verifyNoInteractions(outbox, worker, booking);
    }

    @Test void ownerCanReadPaymentThroughEveryLookup() {
        Payment owned = Payment.builder().id(30L).userId(1L).bookingId(7L).foodOrderId(8L).build();
        var response = mock(com.cinemaai.payment.dto.response.PaymentResponse.class);
        PaymentMapper mapper = (PaymentMapper) org.springframework.test.util.ReflectionTestUtils.getField(service, "paymentMapper");
        when(payments.findById(30L)).thenReturn(Optional.of(owned));
        when(payments.findByBookingId(7L)).thenReturn(Optional.of(owned));
        when(payments.findByFoodOrderId(8L)).thenReturn(Optional.of(owned));
        when(mapper.toResponse(owned)).thenReturn(response);

        assertSame(response, service.getPaymentById(1L, 30L));
        assertSame(response, service.getPaymentByBooking(1L, 7L));
        assertSame(response, service.getPaymentByFoodOrder(1L, 8L));
    }

    @Test void foreignOrdersCannotBePaidAndProduceNoSideEffects() {
        when(booking.getBooking(7L)).thenReturn(new BookingClient.BookingInfo(7L, "BK7", 2L,
                BigDecimal.TEN, "PENDING_PAYMENT", LocalDateTime.now().plusMinutes(2), 0, 1L));
        when(booking.getFoodOrder(8L)).thenReturn(new BookingClient.FoodOrderInfo(8L, "FO8", 2L,
                BigDecimal.TEN, "PENDING_PAYMENT"));
        assertThrows(NotFoundException.class, () -> service.mockPayment(1L, 7L, null));
        assertThrows(NotFoundException.class, () -> service.mockPayment(1L, null, 8L));
        assertThrows(NotFoundException.class, () -> service.createVnpayPayment(1L, null, 8L, "127.0.0.1"));
        verify(payments, never()).save(any());
        verifyNoInteractions(outbox, worker);
        verify(booking, never()).markBookingPaid(anyLong(), anyString(), any());
        verify(booking, never()).markFoodOrderPaid(anyLong(), anyString(), any());
    }

    @Test void successfulMockPaymentRetryHasNoDuplicateSideEffects() {
        Payment succeeded = Payment.builder().id(30L).userId(1L).bookingId(7L)
                .status(PaymentStatus.SUCCESS).amount(BigDecimal.TEN).build();
        var response = mock(com.cinemaai.payment.dto.response.PaymentResponse.class);
        PaymentMapper mapper = (PaymentMapper) org.springframework.test.util.ReflectionTestUtils.getField(service, "paymentMapper");
        when(booking.getBooking(7L)).thenReturn(new BookingClient.BookingInfo(7L, "BK7", 1L,
                BigDecimal.TEN, "PAID", null, 0, 1L));
        when(payments.findByBookingId(7L)).thenReturn(Optional.of(succeeded));
        when(mapper.toResponse(succeeded)).thenReturn(response);

        assertSame(response, service.mockPayment(1L, 7L, null));
        verify(payments, never()).save(any());
        verifyNoInteractions(outbox, worker);
        verify(booking, never()).markBookingPaid(anyLong(), anyString(), any());
    }

    @Test void successfulMockPaymentRetryIgnoresExpiredHoldAndBookingServiceAvailability() {
        Payment succeeded = Payment.builder().id(30L).userId(1L).bookingId(7L)
                .status(PaymentStatus.SUCCESS).amount(BigDecimal.TEN).build();
        var response = mock(com.cinemaai.payment.dto.response.PaymentResponse.class);
        PaymentMapper mapper = (PaymentMapper) org.springframework.test.util.ReflectionTestUtils.getField(service, "paymentMapper");
        when(payments.findByBookingId(7L)).thenReturn(Optional.of(succeeded));
        when(mapper.toResponse(succeeded)).thenReturn(response);

        assertSame(response, service.mockPayment(1L, 7L, null));

        verifyNoInteractions(booking, outbox, worker);
        verify(payments, never()).save(any());
    }

    @Test void foreignUserCannotRetrySuccessfulMockPayment() {
        Payment succeeded = Payment.builder().id(30L).userId(1L).bookingId(7L)
                .status(PaymentStatus.SUCCESS).amount(BigDecimal.TEN).build();
        when(payments.findByBookingId(7L)).thenReturn(Optional.of(succeeded));

        assertThrows(NotFoundException.class, () -> service.mockPayment(2L, 7L, null));

        verifyNoInteractions(booking, outbox, worker);
        verify(payments, never()).save(any());
    }

    @Test void pendingMockPaymentStillCompletesWithExistingSideEffects() {
        assertIncompletePaymentStillCompletes(PaymentStatus.PENDING);
    }

    @Test void failedMockPaymentStillCompletesWithExistingSideEffects() {
        assertIncompletePaymentStillCompletes(PaymentStatus.FAILED);
    }

    @Test void requestWithBothOrderIdentifiersIsRejectedBeforeRepositoryAccess() {
        assertThrows(com.cinemaai.payment.exception.BadRequestException.class,
                () -> service.mockPayment(1L, 7L, 8L));
        verifyNoInteractions(payments, booking, outbox, worker);
    }

    private void assertIncompletePaymentStillCompletes(PaymentStatus initialStatus) {
        Payment existing = Payment.builder().id(30L).userId(1L).bookingId(7L)
                .provider(PaymentProvider.MOCK).status(initialStatus).amount(BigDecimal.TEN).build();
        when(payments.findByBookingId(7L)).thenReturn(Optional.of(existing));
        when(payments.save(existing)).thenReturn(existing);
        when(booking.getBooking(7L)).thenReturn(new BookingClient.BookingInfo(7L, "BK7", 1L,
                BigDecimal.TEN, "PENDING_PAYMENT", LocalDateTime.now().plusMinutes(2), 0, 1L));

        service.mockPayment(1L, 7L, null);

        org.junit.jupiter.api.Assertions.assertEquals(PaymentStatus.SUCCESS, existing.getStatus());
        verify(payments).save(existing);
        verify(outbox).save(any());
        verify(worker).publishPendingEvents();
        verify(booking).markBookingPaid(eq(7L), anyString(), any());
    }
}
