package com.cinemaai.payment.service;

import com.cinemaai.payment.client.BookingClient;
import com.cinemaai.payment.exception.BadRequestException;
import com.cinemaai.payment.entity.Payment;
import com.cinemaai.payment.enums.PaymentProvider;
import com.cinemaai.payment.enums.PaymentStatus;
import com.cinemaai.payment.mapper.PaymentMapper;
import com.cinemaai.payment.repository.OutboxEventRepository;
import com.cinemaai.payment.repository.PaymentRepository;
import com.cinemaai.payment.service.impl.PaymentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentLoyaltyGuardTest {
    private PaymentRepository payments;
    private OutboxEventRepository outbox;
    private OutboxPublisherWorker worker;
    private BookingClient booking;
    private LoyaltyService loyalty;
    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentRepository.class);
        outbox = mock(OutboxEventRepository.class);
        worker = mock(OutboxPublisherWorker.class);
        booking = mock(BookingClient.class);
        loyalty = mock(LoyaltyService.class);
        service = new PaymentServiceImpl(payments, outbox, worker, booking, loyalty,
                new ObjectMapper().findAndRegisterModules(), mock(PaymentMapper.class));
        when(booking.getBooking(7L)).thenReturn(new BookingClient.BookingInfo(
                7L, "BK7", 1L, BigDecimal.TEN, "PENDING_PAYMENT",
                LocalDateTime.now().plusMinutes(2), 500, 1L));
    }

    @Test
    void vnpayCreateRejectsLegacyDiscountBeforePaymentPersistence() {
        assertThrows(BadRequestException.class,
                () -> service.createVnpayPayment(1L, 7L, null, "127.0.0.1"));

        verifyNoInteractions(payments, outbox, worker, loyalty);
        verify(booking, never()).markBookingPaid(anyLong(), any(), any());
    }

    @Test
    void mockPaymentRejectsLegacyDiscountBeforeSuccessOrSideEffects() {
        assertThrows(BadRequestException.class, () -> service.mockPayment(1L, 7L, null));

        verify(payments, never()).save(any());
        verifyNoInteractions(outbox, worker, loyalty);
        verify(booking, never()).markBookingPaid(anyLong(), any(), any());
    }

    @Test
    void capturedVnpayPaymentIsPreservedForReconciliationWithoutTicketsOrLoyalty() {
        Payment pending = Payment.builder().id(42L).bookingId(7L).userId(1L)
                .provider(PaymentProvider.VNPAY).amount(BigDecimal.TEN).status(PaymentStatus.PENDING).build();
        when(payments.findById(42L)).thenReturn(Optional.of(pending));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "vnpHashSecret", "secret");
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "42-BK7");
        params.put("vnp_Amount", "1000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "VNP-1");
        params.put("vnp_SecureHash", com.cinemaai.payment.util.VNPayUtil.hashAllFields(params, "secret"));

        Map<String, String> result = service.processVnpayIpn(params);

        org.junit.jupiter.api.Assertions.assertEquals("00", result.get("RspCode"));
        org.junit.jupiter.api.Assertions.assertEquals(PaymentStatus.SUCCESS, pending.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(pending.getCallbackPayload());
        verify(payments).save(pending);
        verifyNoInteractions(outbox, worker, loyalty);
        verify(booking, never()).markBookingPaid(anyLong(), any(), any());

        service.processVnpayIpn(params);
        verify(booking, never()).markBookingPaid(anyLong(), any(), any());
        verifyNoInteractions(outbox, worker, loyalty);
    }
}
