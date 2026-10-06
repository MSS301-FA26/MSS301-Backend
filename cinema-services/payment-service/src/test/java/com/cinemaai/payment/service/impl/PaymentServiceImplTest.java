package com.cinemaai.payment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinemaai.payment.client.BookingClient;
import com.cinemaai.payment.entity.OutboxEvent;
import com.cinemaai.payment.entity.Payment;
import com.cinemaai.payment.enums.PaymentProvider;
import com.cinemaai.payment.enums.PaymentStatus;
import com.cinemaai.payment.repository.OutboxEventRepository;
import com.cinemaai.payment.repository.PaymentRepository;
import com.cinemaai.payment.service.OutboxPublisherWorker;
import com.cinemaai.payment.util.VNPayUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxPublisherWorker outboxPublisherWorker;

    @Mock
    private BookingClient bookingClient;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                outboxEventRepository,
                outboxPublisherWorker,
                bookingClient,
                new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(paymentService, "vnpHashSecret", "test-secret");
    }

    @Test
    void mockPaymentDoesNotCreateAnotherSuccessEventForAnAlreadySuccessfulPayment() {
        Payment successfulPayment = payment(10L, PaymentStatus.SUCCESS, BigDecimal.valueOf(150_000));
        when(paymentRepository.findByBookingIdForUpdate(99L)).thenReturn(Optional.of(successfulPayment));
        when(bookingClient.getBooking(99L)).thenReturn(new BookingClient.BookingInfo(
                99L, "BK99", 7L, BigDecimal.valueOf(150_000), "PENDING_PAYMENT"));

        paymentService.mockPayment(7L, 99L, null);

        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
        verify(outboxPublisherWorker, never()).publishPendingEvents();
    }

    @Test
    void ipnWithWrongAmountIsRejectedWithoutChangingPayment() {
        Payment pendingPayment = payment(10L, PaymentStatus.PENDING, BigDecimal.valueOf(150_000));
        when(paymentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(pendingPayment));
        Map<String, String> params = signedParams(Map.of(
                "vnp_TxnRef", "10-BK99",
                "vnp_Amount", "14999999",
                "vnp_ResponseCode", "00"));

        Map<String, String> response = paymentService.processVnpayIpn(params);

        assertEquals("04", response.get("RspCode"));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void successfulIpnRetryWritesOnlyOnePaymentSucceededEvent() {
        Payment pendingPayment = payment(10L, PaymentStatus.PENDING, BigDecimal.valueOf(150_000));
        when(paymentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Map<String, String> params = signedParams(Map.of(
                "vnp_TxnRef", "10-BK99",
                "vnp_Amount", "15000000",
                "vnp_ResponseCode", "00",
                "vnp_TransactionNo", "VNP-123"));

        assertEquals("00", paymentService.processVnpayIpn(params).get("RspCode"));
        assertEquals("02", paymentService.processVnpayIpn(params).get("RspCode"));

        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
        verify(outboxPublisherWorker, times(1)).publishPendingEvents();
    }

    @Test
    void ipnWithInvalidSignatureDoesNotLoadOrChangePayment() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "10-BK99");
        params.put("vnp_Amount", "15000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_SecureHash", "invalid");

        Map<String, String> response = paymentService.processVnpayIpn(params);

        assertEquals("97", response.get("RspCode"));
        verify(paymentRepository, never()).findByIdForUpdate(any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    private Map<String, String> signedParams(Map<String, String> input) {
        Map<String, String> params = new HashMap<>(input);
        params.put("vnp_SecureHash", VNPayUtil.hashAllFields(params, "test-secret"));
        return params;
    }

    private Payment payment(Long id, PaymentStatus status, BigDecimal amount) {
        Payment payment = Payment.builder()
                .bookingId(99L)
                .userId(7L)
                .provider(PaymentProvider.MOCK)
                .amount(amount)
                .status(status)
                .build();
        payment.setId(id);
        return payment;
    }
}
