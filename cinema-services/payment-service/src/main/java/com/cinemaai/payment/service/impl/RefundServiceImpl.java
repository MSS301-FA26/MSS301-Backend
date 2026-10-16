package com.cinemaai.payment.service.impl;

import com.cinemaai.payment.client.BookingClient;
import com.cinemaai.payment.dto.request.CreateRefundBatchRequest;
import com.cinemaai.payment.dto.request.CreateRefundRequest;
import com.cinemaai.payment.dto.response.PageResponse;
import com.cinemaai.payment.dto.response.RefundBatchResponse;
import com.cinemaai.payment.dto.response.RefundResponse;
import com.cinemaai.payment.entity.CineWallet;
import com.cinemaai.payment.entity.Payment;
import com.cinemaai.payment.entity.Refund;
import com.cinemaai.payment.entity.RefundBatch;
import com.cinemaai.payment.entity.WalletTransaction;
import com.cinemaai.payment.enums.PaymentStatus;
import com.cinemaai.payment.enums.RefundBatchStatus;
import com.cinemaai.payment.enums.RefundStatus;
import com.cinemaai.payment.enums.WalletTransactionType;
import com.cinemaai.payment.exception.BadRequestException;
import com.cinemaai.payment.exception.NotFoundException;
import com.cinemaai.payment.repository.CineWalletRepository;
import com.cinemaai.payment.repository.PaymentRepository;
import com.cinemaai.payment.repository.RefundBatchRepository;
import com.cinemaai.payment.repository.RefundRepository;
import com.cinemaai.payment.repository.WalletTransactionRepository;
import com.cinemaai.payment.service.RefundService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final RefundBatchRepository refundBatchRepository;
    private final PaymentRepository paymentRepository;
    private final CineWalletRepository cineWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final BookingClient bookingClient;

    @Override
    @Transactional
    public RefundResponse processSingleRefund(CreateRefundRequest request, Long actorUserId) {
        if (request == null) {
            throw new BadRequestException("Yêu cầu hoàn tiền không hợp lệ.");
        }

        Payment payment = null;
        if (request.paymentId() != null) {
            payment = paymentRepository.findById(request.paymentId()).orElse(null);
        }
        if (payment == null && request.bookingId() != null) {
            payment = paymentRepository.findByBookingId(request.bookingId()).orElse(null);
        }
        if (payment == null && request.foodOrderId() != null) {
            payment = paymentRepository.findByFoodOrderId(request.foodOrderId()).orElse(null);
        }

        Long targetUserId = null;
        BigDecimal originalAmount = null;

        if (payment != null) {
            targetUserId = payment.getUserId();
            originalAmount = payment.getAmount();
        } else if (request.bookingId() != null) {
            try {
                BookingClient.BookingInfo b = bookingClient.getBooking(request.bookingId());
                if (b != null) {
                    targetUserId = b.userId();
                    originalAmount = b.totalAmount();
                }
            } catch (Exception ex) {
                log.warn("Could not query booking-service for booking #{}: {}", request.bookingId(), ex.getMessage());
            }
        }

        if (targetUserId == null) {
            throw new BadRequestException("Không thể xác định người dùng nhận tiền hoàn.");
        }

        BigDecimal refundAmt = request.amount();
        if (refundAmt == null || refundAmt.compareTo(BigDecimal.ZERO) <= 0) {
            refundAmt = originalAmount != null ? originalAmount : BigDecimal.ZERO;
        }

        if (refundAmt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Số tiền hoàn phải lớn hơn 0.");
        }

        // Section VII: Refund Business Rule - remaining refundable amount check
        BigDecimal remainingRefundable = refundAmt;
        if (payment != null) {
            BigDecimal completedRefunds = refundRepository.sumCompletedRefundsByPaymentId(payment.getId());
            remainingRefundable = payment.getAmount().subtract(completedRefunds).max(BigDecimal.ZERO);
            if (refundAmt.compareTo(remainingRefundable) > 0) {
                throw new BadRequestException("Số tiền yêu cầu hoàn (" + refundAmt + " VND) vượt quá số tiền còn có thể hoàn ("
                        + remainingRefundable + " VND) của giao dịch #" + payment.getId());
            }
        }

        return executeRefundInternal(payment, request.bookingId(), request.foodOrderId(), targetUserId, refundAmt,
                request.reason(), actorUserId, null);
    }

    @Override
    @Transactional
    public RefundResponse processRefundForBooking(Long bookingId, BigDecimal amount, String reason, Long actorUserId) {
        return processSingleRefund(new CreateRefundRequest(null, bookingId, null, amount, reason), actorUserId);
    }

    @Override
    @Transactional
    public RefundBatchResponse processRefundBatch(CreateRefundBatchRequest request, Long actorUserId) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Danh sách các đơn đặt vé cần hoàn trong batch không được để trống.");
        }

        String batchCode = "RB-" + (request.showtimeId() != null ? "ST" + request.showtimeId() + "-" : "")
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        RefundBatch batch = RefundBatch.builder()
                .batchCode(batchCode)
                .cinemaId(request.cinemaId())
                .showtimeId(request.showtimeId())
                .reason(request.reason() != null ? request.reason() : "Hủy suất chiếu do sự cố vận hành")
                .totalBookings(request.items().size())
                .totalAmount(BigDecimal.ZERO)
                .status(RefundBatchStatus.PROCESSING)
                .createdBy(actorUserId)
                .approvedBy(actorUserId)
                .build();
        batch = refundBatchRepository.save(batch);

        BigDecimal totalRefunded = BigDecimal.ZERO;
        List<Refund> batchRefunds = new ArrayList<>();

        for (CreateRefundBatchRequest.BatchItem item : request.items()) {
            try {
                Payment payment = paymentRepository.findByBookingId(item.bookingId()).orElse(null);
                Long userId = item.userId();
                if (userId == null && payment != null) {
                    userId = payment.getUserId();
                }
                if (userId == null) {
                    continue;
                }

                BigDecimal amt = item.amount();
                if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) {
                    if (payment != null) amt = payment.resolveRemainingRefundable();
                }

                Refund refund = doExecuteRefund(payment, item.bookingId(), null, userId, amt,
                        batch.getReason(), actorUserId, batch);
                totalRefunded = totalRefunded.add(refund.getAmount());
                batchRefunds.add(refund);
            } catch (Exception ex) {
                log.error("Failed to process refund item in batch {} for booking #{}: {}", batchCode, item.bookingId(), ex.getMessage());
            }
        }

        batch.setTotalAmount(totalRefunded);
        batch.setStatus(RefundBatchStatus.COMPLETED);
        batch.setCompletedAt(LocalDateTime.now());
        batch = refundBatchRepository.save(batch);

        log.info("Completed RefundBatch {} for showtime #{}: {} bookings refunded, total: {} VND",
                batchCode, request.showtimeId(), batchRefunds.size(), totalRefunded);

        return RefundBatchResponse.from(batch);
    }

    private RefundResponse executeRefundInternal(
            Payment payment,
            Long bookingId,
            Long foodOrderId,
            Long userId,
            BigDecimal amount,
            String reason,
            Long actorUserId,
            RefundBatch batch
    ) {
        Refund refund = doExecuteRefund(payment, bookingId, foodOrderId, userId, amount, reason, actorUserId, batch);
        return RefundResponse.from(refund);
    }

    private Refund doExecuteRefund(
            Payment payment,
            Long bookingId,
            Long foodOrderId,
            Long userId,
            BigDecimal amount,
            String reason,
            Long actorUserId,
            RefundBatch batch
    ) {
        LocalDateTime now = LocalDateTime.now();
        String refundCode = "RF-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        // 1. Credit user's CineWallet with pessimistic lock
        CineWallet wallet = cineWalletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> cineWalletRepository.save(CineWallet.builder()
                        .userId(userId)
                        .balance(BigDecimal.ZERO)
                        .build()));

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        wallet.setBalance(balanceAfter);
        cineWalletRepository.save(wallet);

        // 2. Record ledger WalletTransaction (REFUND_CREDIT)
        String refCode = "REFUND-" + (bookingId != null ? "BK-" + bookingId : "FO-" + foodOrderId)
                + "-" + System.currentTimeMillis();
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .userId(userId)
                .bookingId(bookingId)
                .type(WalletTransactionType.REFUND_CREDIT)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .referenceType(bookingId != null ? "BOOKING_REFUND" : "FOOD_REFUND")
                .referenceId(bookingId != null ? bookingId : foodOrderId)
                .referenceCode(refCode)
                .description("Hoàn tiền vào ví CineWallet: " + (reason != null ? reason : ""))
                .build();
        walletTransactionRepository.save(tx);

        // 3. Update Payment entity state
        BigDecimal remaining = BigDecimal.ZERO;
        if (payment != null) {
            BigDecimal prevRefunded = payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO;
            BigDecimal newTotalRefunded = prevRefunded.add(amount);
            payment.setRefundAmount(newTotalRefunded);
            payment.setRefundedAt(now);

            remaining = payment.getAmount().subtract(newTotalRefunded).max(BigDecimal.ZERO);
            payment.setRemainingRefundableAmount(remaining);

            if (remaining.compareTo(BigDecimal.ZERO) == 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
            paymentRepository.save(payment);
        }

        // 4. Create Refund record
        Refund refund = Refund.builder()
                .refundCode(refundCode)
                .paymentId(payment != null ? payment.getId() : null)
                .bookingId(bookingId)
                .foodOrderId(foodOrderId)
                .userId(userId)
                .amount(amount)
                .remainingRefundableAmount(remaining)
                .reason(reason != null ? reason : "Hoàn tiền sự cố vận hành rạp")
                .status(RefundStatus.COMPLETED)
                .refundBatch(batch)
                .approvedBy(actorUserId)
                .requestedAt(now)
                .approvedAt(now)
                .completedAt(now)
                .build();
        return refundRepository.save(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponse getRefundById(Long refundId) {
        Refund r = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin hoàn tiền #" + refundId));
        return RefundResponse.from(r);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> getMyRefunds(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)));
        Page<RefundResponse> result = refundRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(RefundResponse::from);
        return PageResponse.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> getAllRefunds(String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)));
        Page<RefundResponse> result;
        if (status != null && !status.isBlank()) {
            try {
                RefundStatus rs = RefundStatus.valueOf(status.trim().toUpperCase());
                result = refundRepository.findByStatusOrderByCreatedAtDesc(rs, pageable).map(RefundResponse::from);
            } catch (IllegalArgumentException ex) {
                result = Page.empty(pageable);
            }
        } else {
            result = refundRepository.findAll(pageable).map(RefundResponse::from);
        }
        return PageResponse.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RefundBatchResponse> getRefundBatches(Long cinemaId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)));
        Page<RefundBatchResponse> result;
        if (cinemaId != null) {
            result = refundBatchRepository.findByCinemaIdOrderByCreatedAtDesc(cinemaId, pageable).map(RefundBatchResponse::from);
        } else if (status != null && !status.isBlank()) {
            try {
                RefundBatchStatus bs = RefundBatchStatus.valueOf(status.trim().toUpperCase());
                result = refundBatchRepository.findByStatusOrderByCreatedAtDesc(bs, pageable).map(RefundBatchResponse::from);
            } catch (IllegalArgumentException ex) {
                result = Page.empty(pageable);
            }
        } else {
            result = refundBatchRepository.findAll(pageable).map(RefundBatchResponse::from);
        }
        return PageResponse.from(result);
    }
}
