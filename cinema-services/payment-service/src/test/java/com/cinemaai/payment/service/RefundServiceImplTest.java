package com.cinemaai.payment.service;

import com.cinemaai.payment.client.BookingClient;
import com.cinemaai.payment.dto.request.CreateRefundBatchRequest;
import com.cinemaai.payment.dto.request.CreateRefundRequest;
import com.cinemaai.payment.dto.response.RefundBatchResponse;
import com.cinemaai.payment.dto.response.RefundResponse;
import com.cinemaai.payment.entity.CineWallet;
import com.cinemaai.payment.entity.Payment;
import com.cinemaai.payment.entity.Refund;
import com.cinemaai.payment.entity.RefundBatch;
import com.cinemaai.payment.enums.PaymentStatus;
import com.cinemaai.payment.enums.RefundBatchStatus;
import com.cinemaai.payment.enums.RefundStatus;
import com.cinemaai.payment.enums.WalletTransactionType;
import com.cinemaai.payment.exception.BadRequestException;
import com.cinemaai.payment.repository.CineWalletRepository;
import com.cinemaai.payment.repository.PaymentRepository;
import com.cinemaai.payment.repository.RefundBatchRepository;
import com.cinemaai.payment.repository.RefundRepository;
import com.cinemaai.payment.repository.WalletTransactionRepository;
import com.cinemaai.payment.service.impl.RefundServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private RefundBatchRepository refundBatchRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CineWalletRepository cineWalletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private BookingClient bookingClient;

    @InjectMocks
    private RefundServiceImpl refundService;

    @Test
    @DisplayName("Single Refund - Full refund updates payment to REFUNDED and credits wallet")
    void testProcessSingleRefund_FullRefund() {
        Long paymentId = 1L;
        Long bookingId = 100L;
        Long userId = 5L;
        BigDecimal amount = BigDecimal.valueOf(150_000);

        Payment payment = Payment.builder()
                .id(paymentId)
                .bookingId(bookingId)
                .userId(userId)
                .amount(amount)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(refundRepository.sumCompletedRefundsByPaymentId(paymentId)).thenReturn(BigDecimal.ZERO);

        CineWallet wallet = CineWallet.builder()
                .id(10L)
                .userId(userId)
                .balance(BigDecimal.valueOf(20_000))
                .build();
        when(cineWalletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund r = invocation.getArgument(0);
            r.setId(77L);
            return r;
        });

        CreateRefundRequest request = new CreateRefundRequest(paymentId, bookingId, null, amount, "Hủy vé");
        RefundResponse response = refundService.processSingleRefund(request, 1L);

        assertNotNull(response);
        assertEquals(RefundStatus.COMPLETED, response.status());
        assertEquals(amount, response.amount());

        // Verify CineWallet balance credited
        assertEquals(BigDecimal.valueOf(170_000), wallet.getBalance());
        verify(cineWalletRepository).save(wallet);

        // Verify WalletTransaction recorded as REFUND_CREDIT
        verify(walletTransactionRepository).save(argThat(tx ->
                tx.getType() == WalletTransactionType.REFUND_CREDIT
                        && tx.getAmount().compareTo(amount) == 0
                        && tx.getBalanceBefore().compareTo(BigDecimal.valueOf(20_000)) == 0
                        && tx.getBalanceAfter().compareTo(BigDecimal.valueOf(170_000)) == 0
        ));

        // Verify Payment updated to REFUNDED
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals(BigDecimal.ZERO, payment.getRemainingRefundableAmount());
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("Single Refund - Partial refund updates payment to PARTIALLY_REFUNDED")
    void testProcessSingleRefund_PartialRefund() {
        Long paymentId = 1L;
        Long bookingId = 100L;
        Long userId = 5L;
        BigDecimal totalAmount = BigDecimal.valueOf(200_000);
        BigDecimal partialAmount = BigDecimal.valueOf(80_000);

        Payment payment = Payment.builder()
                .id(paymentId)
                .bookingId(bookingId)
                .userId(userId)
                .amount(totalAmount)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(refundRepository.sumCompletedRefundsByPaymentId(paymentId)).thenReturn(BigDecimal.ZERO);

        CineWallet wallet = CineWallet.builder()
                .id(10L)
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .build();
        when(cineWalletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund r = invocation.getArgument(0);
            r.setId(78L);
            return r;
        });

        CreateRefundRequest request = new CreateRefundRequest(paymentId, bookingId, null, partialAmount, "Hoàn 1 phần vé");
        RefundResponse response = refundService.processSingleRefund(request, 1L);

        assertNotNull(response);
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, payment.getStatus());
        assertEquals(BigDecimal.valueOf(120_000), payment.getRemainingRefundableAmount());
        assertEquals(BigDecimal.valueOf(80_000), wallet.getBalance());
    }

    @Test
    @DisplayName("Single Refund - Exceeding remaining refundable amount throws BadRequestException")
    void testProcessSingleRefund_ExceedingRemaining() {
        Long paymentId = 1L;
        Long userId = 5L;
        BigDecimal totalAmount = BigDecimal.valueOf(100_000);

        Payment payment = Payment.builder()
                .id(paymentId)
                .userId(userId)
                .amount(totalAmount)
                .status(PaymentStatus.PARTIALLY_REFUNDED)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        // Already refunded 60,000, so remaining is 40,000
        when(refundRepository.sumCompletedRefundsByPaymentId(paymentId)).thenReturn(BigDecimal.valueOf(60_000));

        // Requesting 50,000 > 40,000 remaining
        CreateRefundRequest request = new CreateRefundRequest(paymentId, null, null, BigDecimal.valueOf(50_000), "Hoàn quá số tiền");

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                refundService.processSingleRefund(request, 1L));

        assertTrue(ex.getMessage().contains("vượt quá"));
        verify(cineWalletRepository, never()).save(any());
    }

    @Test
    @DisplayName("Batch Refund - Processes all items and marks batch COMPLETED")
    void testProcessRefundBatch() {
        Long showtimeId = 42L;
        Long cinemaId = 1L;
        Long actorUserId = 9L;

        CreateRefundBatchRequest.BatchItem item1 = new CreateRefundBatchRequest.BatchItem(
                101L, 11L, BigDecimal.valueOf(100_000), "BK-101");
        CreateRefundBatchRequest.BatchItem item2 = new CreateRefundBatchRequest.BatchItem(
                102L, 12L, BigDecimal.valueOf(120_000), "BK-102");

        CreateRefundBatchRequest batchRequest = new CreateRefundBatchRequest(
                cinemaId, showtimeId, "Hủy suất chiếu sự cố máy chiếu", List.of(item1, item2));

        when(refundBatchRepository.save(any(RefundBatch.class))).thenAnswer(invocation -> {
            RefundBatch b = invocation.getArgument(0);
            b.setId(500L);
            return b;
        });

        Payment p1 = Payment.builder().id(1L).bookingId(101L).userId(11L).amount(BigDecimal.valueOf(100_000)).build();
        Payment p2 = Payment.builder().id(2L).bookingId(102L).userId(12L).amount(BigDecimal.valueOf(120_000)).build();
        when(paymentRepository.findByBookingId(101L)).thenReturn(Optional.of(p1));
        when(paymentRepository.findByBookingId(102L)).thenReturn(Optional.of(p2));

        when(cineWalletRepository.findByUserIdForUpdate(anyLong())).thenAnswer(inv -> {
            Long uid = inv.getArgument(0);
            return Optional.of(CineWallet.builder().userId(uid).balance(BigDecimal.ZERO).build());
        });

        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefundBatchResponse response = refundService.processRefundBatch(batchRequest, actorUserId);

        assertNotNull(response);
        assertEquals(RefundBatchStatus.COMPLETED, response.status());
        assertEquals(BigDecimal.valueOf(220_000), response.totalAmount());
        assertEquals(2, response.totalBookings());
        verify(refundBatchRepository, atLeast(2)).save(any(RefundBatch.class));
    }
}
