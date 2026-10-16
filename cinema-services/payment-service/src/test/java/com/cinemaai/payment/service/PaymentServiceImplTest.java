package com.cinemaai.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cinemaai.payment.client.BookingClient;
import com.cinemaai.payment.dto.response.PaymentResponse;
import com.cinemaai.payment.dto.response.VNPayPaymentResponse;
import com.cinemaai.payment.entity.CineWallet;
import com.cinemaai.payment.entity.Payment;
import com.cinemaai.payment.entity.PaymentTransaction;
import com.cinemaai.payment.entity.WalletTransaction;
import com.cinemaai.payment.enums.PaymentProvider;
import com.cinemaai.payment.enums.PaymentStatus;
import com.cinemaai.payment.enums.PaymentTransactionStatus;
import com.cinemaai.payment.enums.WalletTransactionType;
import com.cinemaai.payment.exception.BadRequestException;
import com.cinemaai.payment.repository.CineWalletRepository;
import com.cinemaai.payment.repository.OutboxEventRepository;
import com.cinemaai.payment.repository.PaymentRepository;
import com.cinemaai.payment.repository.PaymentTransactionRepository;
import com.cinemaai.payment.repository.WalletTransactionRepository;
import com.cinemaai.payment.service.impl.PaymentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private CineWalletRepository cineWalletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxPublisherWorker outboxPublisherWorker;

    @Mock
    private BookingClient bookingClient;

    @Mock
    private LoyaltyService loyaltyService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "vnpTmnCode", "TESTCODE");
        ReflectionTestUtils.setField(paymentService, "vnpHashSecret", "TESTSECRET12345678901234567890");
        ReflectionTestUtils.setField(paymentService, "vnpPayUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(paymentService, "vnpReturnUrl", "https://cinema.test/vnpay/return");
    }

    @Test
    @DisplayName("Wallet Payment - Success: Atomic debit, ledger record, payment success")
    void testProcessWalletPayment_Success() {
        Long userId = 10L;
        Long bookingId = 100L;
        BigDecimal amount = BigDecimal.valueOf(150_000);

        BookingClient.BookingInfo bookingInfo = new BookingClient.BookingInfo(
                bookingId, "BK-TEST-001", userId, amount, "PENDING", 0, 1L);
        when(bookingClient.getBooking(bookingId)).thenReturn(bookingInfo);

        CineWallet wallet = CineWallet.builder()
                .id(1L)
                .userId(userId)
                .balance(BigDecimal.valueOf(200_000))
                .build();
        when(cineWalletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        PaymentResponse response = paymentService.processWalletPayment(userId, bookingId, null);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.status());
        assertEquals(amount, response.amount());

        // Verify balance was reduced
        assertEquals(BigDecimal.valueOf(50_000), wallet.getBalance());
        verify(cineWalletRepository).save(wallet);

        // Verify PAYMENT_DEBIT ledger transaction was recorded
        verify(walletTransactionRepository).save(argThat(tx ->
                tx.getType() == WalletTransactionType.PAYMENT_DEBIT
                        && tx.getAmount().compareTo(amount.negate()) == 0
                        && tx.getBalanceBefore().compareTo(BigDecimal.valueOf(200_000)) == 0
                        && tx.getBalanceAfter().compareTo(BigDecimal.valueOf(50_000)) == 0
        ));

        // Verify PaymentTransaction was recorded
        verify(paymentTransactionRepository).save(argThat(pt ->
                pt.getStatus() == PaymentTransactionStatus.SUCCESS
                        && pt.getProvider() == PaymentProvider.CINEWALLET
        ));

        // Verify OutboxEvent was recorded in the same transaction
        verify(outboxEventRepository).save(any());
        verify(bookingClient).markBookingPaid(eq(bookingId), anyString(), any());
    }

    @Test
    @DisplayName("Wallet Payment - Insufficient Balance: Throws BadRequestException")
    void testProcessWalletPayment_InsufficientBalance() {
        Long userId = 10L;
        Long bookingId = 100L;
        BigDecimal amount = BigDecimal.valueOf(150_000);

        BookingClient.BookingInfo bookingInfo = new BookingClient.BookingInfo(
                bookingId, "BK-TEST-001", userId, amount, "PENDING", 0, 1L);
        when(bookingClient.getBooking(bookingId)).thenReturn(bookingInfo);

        CineWallet wallet = CineWallet.builder()
                .id(1L)
                .userId(userId)
                .balance(BigDecimal.valueOf(50_000)) // Less than required
                .build();
        when(cineWalletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                paymentService.processWalletPayment(userId, bookingId, null)
        );

        assertTrue(ex.getMessage().contains("INSUFFICIENT_WALLET_BALANCE"));
        verify(walletTransactionRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("VNPay Create - Records PaymentTransaction with PENDING status and expiration")
    void testCreateVnpayPayment_RecordsTransactionAttempt() {
        Long userId = 10L;
        Long bookingId = 100L;
        BigDecimal amount = BigDecimal.valueOf(120_000);

        BookingClient.BookingInfo bookingInfo = new BookingClient.BookingInfo(
                bookingId, "BK-TEST-002", userId, amount, "PENDING", 0, 1L);
        when(bookingClient.getBooking(bookingId)).thenReturn(bookingInfo);
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(2L);
            return p;
        });

        VNPayPaymentResponse response = paymentService.createVnpayPayment(userId, bookingId, null, "127.0.0.1");

        assertNotNull(response);
        assertNotNull(response.paymentUrl());

        // Verify PaymentTransaction was recorded
        verify(paymentTransactionRepository).save(argThat(tx ->
                tx.getPaymentId().equals(2L)
                        && tx.getProvider() == PaymentProvider.VNPAY
                        && tx.getStatus() == PaymentTransactionStatus.PENDING
                        && tx.getExpiresAt() != null
        ));
    }

    @Test
    @DisplayName("Reconcile Payment - Detects matching and mismatching transaction states")
    void testReconcilePayment() {
        Long paymentId = 1L;
        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.SUCCESS)
                .provider(PaymentProvider.VNPAY)
                .amount(BigDecimal.valueOf(100_000))
                .build();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentTransaction tx = PaymentTransaction.builder()
                .paymentId(paymentId)
                .status(PaymentTransactionStatus.SUCCESS)
                .amount(BigDecimal.valueOf(100_000))
                .build();
        when(paymentTransactionRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId)).thenReturn(List.of(tx));

        Map<String, Object> report = paymentService.reconcilePayment(paymentId);
        assertNotNull(report);
        assertEquals(false, report.get("mismatchDetected"));
        assertEquals(true, report.get("hasSuccessfulTransaction"));
    }
}
