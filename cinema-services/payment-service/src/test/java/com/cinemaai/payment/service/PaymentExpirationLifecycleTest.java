package com.cinemaai.payment.service;

import com.cinemaai.payment.client.BookingClient;
import com.cinemaai.payment.entity.Payment;
import com.cinemaai.payment.enums.PaymentProvider;
import com.cinemaai.payment.enums.PaymentStatus;
import com.cinemaai.payment.exception.BadRequestException;
import com.cinemaai.payment.mapper.PaymentMapper;
import com.cinemaai.payment.repository.OutboxEventRepository;
import com.cinemaai.payment.repository.PaymentRepository;
import com.cinemaai.payment.service.impl.PaymentServiceImpl;
import com.cinemaai.payment.util.VNPayUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentExpirationLifecycleTest {
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
        ReflectionTestUtils.setField(service, "vnpHashSecret", "secret");
    }

    @Test
    void mockPaymentRejectsCancelledBookingBeforePaymentMutation() {
        when(booking.getBooking(7L)).thenReturn(info("CANCELLED", LocalDateTime.now().plusMinutes(1)));
        assertThrows(BadRequestException.class, () -> service.mockPayment(1L, 7L, null));
        verify(payments, never()).save(any());
        verifyNoInteractions(outbox, worker, loyalty);
        verify(booking, never()).markBookingPaid(anyLong(), any(), any());
    }

    @Test
    void lateCapturedVnpayPaymentRemainsSuccessWithEvidenceAndNoFulfilment() {
        Payment pending = Payment.builder().id(42L).bookingId(7L).userId(1L)
                .provider(PaymentProvider.VNPAY).amount(BigDecimal.TEN).status(PaymentStatus.PENDING).build();
        when(payments.findById(42L)).thenReturn(Optional.of(pending));
        when(booking.getBooking(7L)).thenReturn(info("EXPIRED", LocalDateTime.now().minusMinutes(1)));

        Map<String, String> result = service.processVnpayIpn(successParams());

        assertEquals("00", result.get("RspCode"));
        assertEquals(PaymentStatus.SUCCESS, pending.getStatus());
        assertEquals("VNP-42", pending.getTransactionId());
        assertNotNull(pending.getPaidAt());
        assertNotNull(pending.getCallbackPayload());
        verify(payments).save(pending);
        verifyNoInteractions(outbox, worker, loyalty);
        verify(booking, never()).markBookingPaid(anyLong(), any(), any());
    }

    @Test
    void retryOfLateCapturedPaymentDoesNotAttemptFulfilmentAgain() {
        Payment captured = Payment.builder().id(42L).bookingId(7L).userId(1L)
                .provider(PaymentProvider.VNPAY).amount(BigDecimal.TEN).status(PaymentStatus.SUCCESS)
                .transactionId("VNP-42").paidAt(LocalDateTime.now()).build();
        when(payments.findById(42L)).thenReturn(Optional.of(captured));
        when(booking.getBooking(7L)).thenReturn(info("EXPIRED", LocalDateTime.now().minusMinutes(1)));

        service.processVnpayIpn(successParams());

        verify(booking, never()).markBookingPaid(anyLong(), any(), any());
        verifyNoInteractions(outbox, worker, loyalty);
    }

    @Test
    void outboxSerializationFailureAbortsFulfilmentInsteadOfBeingIgnored() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});
        service = new PaymentServiceImpl(payments, outbox, worker, booking, loyalty,
                failingMapper, mock(PaymentMapper.class));
        ReflectionTestUtils.setField(service, "vnpHashSecret", "secret");
        Payment pending = Payment.builder().id(42L).bookingId(7L).userId(1L)
                .provider(PaymentProvider.VNPAY).amount(BigDecimal.TEN).status(PaymentStatus.PENDING).build();
        when(payments.findById(42L)).thenReturn(Optional.of(pending));
        when(booking.getBooking(7L)).thenReturn(info("PENDING_PAYMENT", LocalDateTime.now().plusMinutes(1)));

        assertThrows(IllegalStateException.class, () -> service.processVnpayIpn(successParams()));

        verify(outbox, never()).save(any());
        verify(booking, never()).markBookingPaid(anyLong(), any(), any());
        verifyNoInteractions(worker, loyalty);
    }

    @Test
    void externalFulfilmentIsDeferredUntilPaymentTransactionCommits() {
        Payment pending = Payment.builder().id(42L).bookingId(7L).userId(1L)
                .provider(PaymentProvider.VNPAY).amount(BigDecimal.TEN).status(PaymentStatus.PENDING).build();
        when(payments.findById(42L)).thenReturn(Optional.of(pending));
        when(booking.getBooking(7L)).thenReturn(info("PENDING_PAYMENT", LocalDateTime.now().plusMinutes(1)));

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            service.processVnpayIpn(successParams());

            verify(booking, never()).markBookingPaid(anyLong(), any(), any());
            verifyNoInteractions(worker, loyalty);

            TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());
            InOrder postCommitOrder = inOrder(worker, booking);
            postCommitOrder.verify(worker).publishPendingEvents();
            postCommitOrder.verify(booking).markBookingPaid(eq(7L), eq("VNP-42"), any());
            verifyNoInteractions(loyalty);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void rollbackDoesNotCallBookingOrLoyalty() {
        Payment pending = Payment.builder().id(42L).bookingId(7L).userId(1L)
                .provider(PaymentProvider.VNPAY).amount(BigDecimal.TEN).status(PaymentStatus.PENDING).build();
        when(payments.findById(42L)).thenReturn(Optional.of(pending));
        when(booking.getBooking(7L)).thenReturn(info("PENDING_PAYMENT", LocalDateTime.now().plusMinutes(1)));

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            service.processVnpayIpn(successParams());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }

        verify(booking, never()).markBookingPaid(anyLong(), any(), any());
        verifyNoInteractions(worker, loyalty);
    }

    private BookingClient.BookingInfo info(String status, LocalDateTime expiresAt) {
        return new BookingClient.BookingInfo(7L, "BK7", 1L, BigDecimal.TEN, status, expiresAt, 0, 1L);
    }

    private Map<String, String> successParams() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "42-BK7");
        params.put("vnp_Amount", "1000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "VNP-42");
        params.put("vnp_SecureHash", VNPayUtil.hashAllFields(params, "secret"));
        return params;
    }
}
