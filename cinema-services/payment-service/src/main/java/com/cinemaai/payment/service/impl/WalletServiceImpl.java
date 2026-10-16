package com.cinemaai.payment.service.impl;

import com.cinemaai.payment.dto.request.CreditWalletInternalRequest;
import com.cinemaai.payment.dto.request.WithdrawalCreateRequest;
import com.cinemaai.payment.dto.request.WithdrawalProcessRequest;
import com.cinemaai.payment.dto.response.PageResponse;
import com.cinemaai.payment.dto.response.WalletDashboardResponse;
import com.cinemaai.payment.dto.response.WalletResponse;
import com.cinemaai.payment.dto.response.WalletTransactionResponse;
import com.cinemaai.payment.dto.response.WithdrawalResponse;
import com.cinemaai.payment.entity.CineWallet;
import com.cinemaai.payment.entity.WalletTransaction;
import com.cinemaai.payment.entity.WithdrawalRequest;
import com.cinemaai.payment.enums.WalletTransactionType;
import com.cinemaai.payment.enums.WithdrawalStatus;
import com.cinemaai.payment.exception.BadRequestException;
import com.cinemaai.payment.exception.NotFoundException;
import com.cinemaai.payment.provider.PayoutProvider;
import com.cinemaai.payment.provider.PayoutResult;
import com.cinemaai.payment.repository.CineWalletRepository;
import com.cinemaai.payment.repository.WalletTransactionRepository;
import com.cinemaai.payment.repository.WithdrawalRequestRepository;
import com.cinemaai.payment.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
public class WalletServiceImpl implements WalletService {

    private final CineWalletRepository cineWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final PayoutProvider payoutProvider;

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long userId) {
        CineWallet wallet = getOrCreateWallet(userId);
        return new WalletResponse(wallet.getId(), wallet.getBalance(), wallet.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WalletTransactionResponse> getTransactions(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)));
        Page<WalletTransactionResponse> result = walletTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(WalletTransactionResponse::from);
        return PageResponse.from(result);
    }

    @Override
    @Transactional
    public WithdrawalResponse createWithdrawalRequest(Long userId, WithdrawalCreateRequest request) {
        CineWallet wallet = cineWalletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> cineWalletRepository.save(CineWallet.builder()
                        .userId(userId)
                        .balance(BigDecimal.ZERO)
                        .build()));

        BigDecimal amount = request.amount();
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Số dư CineWallet không đủ. Số dư hiện tại: " + wallet.getBalance() + " VND");
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);

        // Deduct balance (hold for withdrawal)
        wallet.setBalance(balanceAfter);
        cineWalletRepository.save(wallet);

        WithdrawalRequest wr = WithdrawalRequest.builder()
                .wallet(wallet)
                .userId(userId)
                .amount(amount)
                .bankName(request.bankName())
                .accountNumber(request.accountNumber())
                .accountHolder(request.accountHolder())
                .walletPhone(request.walletPhone())
                .status(WithdrawalStatus.PROCESSING)
                .build();
        WithdrawalRequest saved = withdrawalRequestRepository.save(wr);

        // Record holding / debit transaction
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .userId(userId)
                .type(WalletTransactionType.WITHDRAWAL_DEBIT)
                .amount(amount.negate())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .referenceType("WITHDRAWAL")
                .referenceId(saved.getId())
                .referenceCode("WD-" + saved.getId())
                .description("Yêu cầu rút tiền về ngân hàng " + request.bankName())
                .build();
        walletTransactionRepository.save(tx);

        log.info("Created withdrawal request #{} for userId: {}, amount: {}. Processing via PayoutProvider...",
                saved.getId(), userId, amount);

        // Execute via MockPayoutProvider
        PayoutResult payoutResult = payoutProvider.processPayout(saved);
        if (payoutResult.success()) {
            saved.setStatus(WithdrawalStatus.COMPLETED);
            saved.setProcessedMethod("MOCK_PAYOUT");
            saved.setProcessedNote("Chuyển khoản thành công qua cổng thanh toán (Mã: " + payoutResult.providerTransactionId() + ")");
            saved.setProcessedAt(LocalDateTime.now());
            withdrawalRequestRepository.save(saved);
            log.info("Withdrawal #{} completed successfully via payout provider.", saved.getId());
        } else {
            // Reversal on failure per payment.md Section XVIII
            saved.setStatus(WithdrawalStatus.FAILED);
            saved.setProcessedNote("Giao dịch rút tiền thất bại: " + payoutResult.failureReason());
            saved.setProcessedAt(LocalDateTime.now());
            withdrawalRequestRepository.save(saved);

            // Restore wallet balance
            BigDecimal restoredBalance = wallet.getBalance().add(amount);
            wallet.setBalance(restoredBalance);
            cineWalletRepository.save(wallet);

            WalletTransaction reversalTx = WalletTransaction.builder()
                    .wallet(wallet)
                    .userId(userId)
                    .type(WalletTransactionType.WITHDRAWAL_REVERSAL)
                    .amount(amount)
                    .balanceBefore(balanceAfter)
                    .balanceAfter(restoredBalance)
                    .referenceType("WITHDRAWAL_REVERSAL")
                    .referenceId(saved.getId())
                    .referenceCode("WD-REV-" + saved.getId())
                    .description("Hoàn lại số dư ví do rút tiền thất bại: " + payoutResult.failureReason())
                    .build();
            walletTransactionRepository.save(reversalTx);

            log.warn("Withdrawal #{} failed and reversed back to wallet. Reason: {}", saved.getId(), payoutResult.failureReason());
        }

        return WithdrawalResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WithdrawalResponse> getMyWithdrawals(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)));
        Page<WithdrawalResponse> result = withdrawalRequestRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(WithdrawalResponse::from);
        return PageResponse.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WithdrawalResponse> getAllWithdrawals(String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)));
        Page<WithdrawalResponse> result;
        if (status != null && !status.isBlank()) {
            try {
                WithdrawalStatus ws = WithdrawalStatus.valueOf(status.trim().toUpperCase());
                result = withdrawalRequestRepository.findByStatusOrderByCreatedAtDesc(ws, pageable)
                        .map(WithdrawalResponse::from);
            } catch (IllegalArgumentException ex) {
                result = Page.empty(pageable);
            }
        } else {
            result = withdrawalRequestRepository.findAllByOrderByCreatedAtDesc(pageable)
                    .map(WithdrawalResponse::from);
        }
        return PageResponse.from(result);
    }

    @Override
    @Transactional
    public WithdrawalResponse approveWithdrawal(Long withdrawalId, WithdrawalProcessRequest request) {
        WithdrawalRequest wr = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu rút tiền: " + withdrawalId));

        if (wr.getStatus().isTerminal()) {
            throw new BadRequestException("Không thể duyệt yêu cầu rút tiền đã ở trạng thái kết thúc: " + wr.getStatus());
        }

        wr.setStatus(WithdrawalStatus.COMPLETED);
        wr.setProcessedMethod(request != null && request.method() != null ? request.method() : "BANK_TRANSFER");
        wr.setProcessedNote(request != null ? request.note() : null);
        wr.setProcessedAt(LocalDateTime.now());
        withdrawalRequestRepository.save(wr);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wr.getWallet())
                .userId(wr.getUserId())
                .type(WalletTransactionType.WITHDRAWAL_PAID)
                .amount(BigDecimal.ZERO)
                .balanceBefore(wr.getWallet().getBalance())
                .balanceAfter(wr.getWallet().getBalance())
                .referenceType("WITHDRAWAL")
                .referenceId(wr.getId())
                .referenceCode("WD-" + wr.getId())
                .description("Đã duyệt chuyển khoản yêu cầu rút #" + wr.getId())
                .build();
        walletTransactionRepository.save(tx);

        log.info("Approved withdrawal request #{}", withdrawalId);
        return WithdrawalResponse.from(wr);
    }

    @Override
    @Transactional
    public WithdrawalResponse rejectWithdrawal(Long withdrawalId, WithdrawalProcessRequest request) {
        WithdrawalRequest wr = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu rút tiền: " + withdrawalId));

        if (wr.getStatus().isTerminal()) {
            throw new BadRequestException("Không thể từ chối yêu cầu rút tiền đã ở trạng thái kết thúc: " + wr.getStatus());
        }

        wr.setStatus(WithdrawalStatus.FAILED);
        wr.setProcessedNote(request != null ? request.note() : "Bị từ chối bởi staff/admin");
        wr.setProcessedAt(LocalDateTime.now());
        withdrawalRequestRepository.save(wr);

        // Refund held money back to user's wallet
        CineWallet wallet = wr.getWallet();
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(wr.getAmount());
        wallet.setBalance(balanceAfter);
        cineWalletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .userId(wr.getUserId())
                .type(WalletTransactionType.WITHDRAWAL_REVERSAL)
                .amount(wr.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .referenceType("WITHDRAWAL_REVERSAL")
                .referenceId(wr.getId())
                .referenceCode("WD-REJECT-" + wr.getId())
                .description("Hoàn lại số dư do yêu cầu rút #" + wr.getId() + " bị từ chối")
                .build();
        walletTransactionRepository.save(tx);

        log.info("Rejected withdrawal request #{} and refunded balance to userId {}", withdrawalId, wr.getUserId());
        return WithdrawalResponse.from(wr);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletDashboardResponse getDashboard() {
        BigDecimal totalBalance = cineWalletRepository.sumBalances();
        BigDecimal totalRefunded = walletTransactionRepository.sumAmountByType(WalletTransactionType.REFUND_CREDIT);
        BigDecimal totalWithdrawn = withdrawalRequestRepository.sumAmountByStatus(WithdrawalStatus.COMPLETED);
        if (totalWithdrawn == null || totalWithdrawn.compareTo(BigDecimal.ZERO) == 0) {
            totalWithdrawn = withdrawalRequestRepository.sumAmountByStatus(WithdrawalStatus.PAID);
        }
        BigDecimal pendingAmount = withdrawalRequestRepository.sumAmountByStatus(WithdrawalStatus.PROCESSING);
        if (pendingAmount == null || pendingAmount.compareTo(BigDecimal.ZERO) == 0) {
            pendingAmount = withdrawalRequestRepository.sumAmountByStatus(WithdrawalStatus.PENDING);
        }
        long pendingCount = withdrawalRequestRepository.countByStatus(WithdrawalStatus.PROCESSING);
        long totalWallets = cineWalletRepository.count();

        Pageable top10 = PageRequest.of(0, 10);
        List<WalletTransactionResponse> recentTx = walletTransactionRepository
                .findAllByOrderByCreatedAtDesc(top10)
                .map(WalletTransactionResponse::from)
                .getContent();

        return new WalletDashboardResponse(
                totalBalance,
                totalRefunded,
                totalWithdrawn,
                pendingAmount,
                pendingCount,
                totalWallets,
                recentTx
        );
    }

    @Override
    @Transactional
    public WalletResponse creditWalletInternal(CreditWalletInternalRequest request) {
        if (request == null || request.userId() == null) {
            throw new BadRequestException("User ID khong duoc de trong");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("So tien hoan vao vi phai lon hon 0");
        }

        String refCode = "REFUND-" + (request.bookingCode() != null && !request.bookingCode().isBlank()
                ? request.bookingCode() : ("BK-" + request.bookingId()));

        if (walletTransactionRepository.existsByReferenceCode(refCode)) {
            log.warn("Giao dich hoan tien da ton tai cho ma tham chieu: {}", refCode);
            CineWallet wallet = cineWalletRepository.findByUserId(request.userId()).orElse(null);
            if (wallet != null) {
                return new WalletResponse(wallet.getId(), wallet.getBalance(), wallet.getCreatedAt());
            }
        }

        CineWallet wallet = cineWalletRepository.findByUserIdForUpdate(request.userId())
                .orElseGet(() -> cineWalletRepository.save(CineWallet.builder()
                        .userId(request.userId())
                        .balance(BigDecimal.ZERO)
                        .build()));

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal newBalance = balanceBefore.add(request.amount());
        wallet.setBalance(newBalance);
        cineWalletRepository.save(wallet);

        String desc = request.reason() != null && !request.reason().isBlank()
                ? request.reason() : ("Hoan tien ve " + (request.bookingCode() != null ? request.bookingCode() : ""));

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .userId(request.userId())
                .bookingId(request.bookingId())
                .type(WalletTransactionType.REFUND_CREDIT)
                .amount(request.amount())
                .balanceBefore(balanceBefore)
                .balanceAfter(newBalance)
                .referenceType("REFUND")
                .referenceId(request.bookingId())
                .referenceCode(refCode)
                .description(desc)
                .build();
        walletTransactionRepository.save(tx);

        log.info("Da hoan {} vao CineWallet cua userId {} (so du moi: {}, ref: {})", request.amount(), request.userId(), newBalance, refCode);
        return new WalletResponse(wallet.getId(), wallet.getBalance(), wallet.getCreatedAt());
    }

    private CineWallet getOrCreateWallet(Long userId) {
        return cineWalletRepository.findByUserId(userId)
                .orElseGet(() -> cineWalletRepository.save(CineWallet.builder()
                        .userId(userId)
                        .balance(BigDecimal.ZERO)
                        .build()));
    }
}
