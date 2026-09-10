package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.wallet.WithdrawalCreateRequest;
import com.sba301.cinemaai.dto.request.wallet.WithdrawalProcessRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.wallet.WalletDashboardResponse;
import com.sba301.cinemaai.dto.response.wallet.WalletResponse;
import com.sba301.cinemaai.dto.response.wallet.WalletTransactionResponse;
import com.sba301.cinemaai.dto.response.wallet.WithdrawalResponse;
import com.sba301.cinemaai.entity.CineWallet;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.entity.WalletTransaction;
import com.sba301.cinemaai.entity.WithdrawalRequest;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.WalletTransactionType;
import com.sba301.cinemaai.enums.WithdrawalStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.CineWalletRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.repository.WalletTransactionRepository;
import com.sba301.cinemaai.repository.WithdrawalRequestRepository;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.WalletService;
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
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    // ─── User Endpoints ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWallet(String email) {
        CineWallet wallet = getOrCreateWallet(findUser(email));
        return new WalletResponse(wallet.getId(), wallet.getBalance(), wallet.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WalletTransactionResponse> getTransactions(String email, int page, int size) {
        User user = findUser(email);
        CineWallet wallet = getOrCreateWallet(user);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)));
        Page<WalletTransactionResponse> result = walletTransactionRepository
                .findByWalletFiltered(wallet, pageable)
                .map(WalletTransactionResponse::from);
        return PageResponse.from(result);
    }

    @Override
    @Transactional
    public WithdrawalResponse createWithdrawalRequest(String email, WithdrawalCreateRequest request) {
        User user = findUser(email);
        CineWallet wallet = cineWalletRepository.findByUserForUpdate(user)
                .orElseGet(() -> cineWalletRepository.save(new CineWallet(user)));

        BigDecimal amount = request.amount();
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Số dư CineWallet không đủ. Số dư hiện tại: " + wallet.getBalance() + " VND");
        }

        // Trừ balance (hold)
        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);
        cineWalletRepository.save(wallet);

        // Tạo wallet transaction WITHDRAWAL_HOLD
        WalletTransaction tx = new WalletTransaction(
                wallet, user, null,
                WalletTransactionType.WITHDRAWAL_HOLD,
                amount.negate(),
                newBalance,
                "WD-" + System.currentTimeMillis(),
                "Yêu cầu rút tiền: " + request.bankName() + " - " + request.accountNumber()
        );
        walletTransactionRepository.save(tx);

        // Tạo withdrawal request
        WithdrawalRequest wr = new WithdrawalRequest(
                wallet, user, amount,
                request.bankName(),
                request.accountNumber(),
                request.accountHolder(),
                request.walletPhone()
        );
        withdrawalRequestRepository.save(wr);

        log.info("Withdrawal request created: {} VND for user {} (bank: {} {})",
                amount, email, request.bankName(), request.accountNumber());

        return WithdrawalResponse.from(wr);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WithdrawalResponse> getMyWithdrawals(String email, int page, int size) {
        User user = findUser(email);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)));
        Page<WithdrawalResponse> result = withdrawalRequestRepository
                .findByUserOrderByCreatedAtDesc(user, pageable)
                .map(WithdrawalResponse::from);
        return PageResponse.from(result);
    }

    // ─── Admin Endpoints ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WithdrawalResponse> getAllWithdrawals(String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)));
        Page<WithdrawalResponse> result;

        if (status != null && !status.isBlank()) {
            WithdrawalStatus ws = WithdrawalStatus.valueOf(status.toUpperCase());
            result = withdrawalRequestRepository
                    .findByStatusOrderByCreatedAtDesc(ws, pageable)
                    .map(WithdrawalResponse::from);
        } else {
            result = withdrawalRequestRepository
                    .findAllByOrderByCreatedAtDesc(pageable)
                    .map(WithdrawalResponse::from);
        }
        return PageResponse.from(result);
    }

    @Override
    @Transactional
    public WithdrawalResponse approveWithdrawal(Long withdrawalId, WithdrawalProcessRequest request) {
        WithdrawalRequest wr = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new NotFoundException("Withdrawal request not found"));

        if (wr.getStatus() != WithdrawalStatus.PENDING) {
            throw new BadRequestException("Only PENDING withdrawal requests can be approved");
        }

        wr.setStatus(WithdrawalStatus.PAID);
        wr.setProcessedMethod(request.method());
        wr.setProcessedNote(request.note());
        wr.setProcessedAt(LocalDateTime.now());
        withdrawalRequestRepository.save(wr);

        log.info("Withdrawal {} approved via {} for user {}", withdrawalId, request.method(), wr.getUser().getEmail());
        auditLogService.record(AuditActionType.APPROVE, "WALLET", wr.getId(),
                wr.getUser().getEmail() + " - rút " + wr.getAmount() + " VND");
        return WithdrawalResponse.from(wr);
    }

    @Override
    @Transactional
    public WithdrawalResponse rejectWithdrawal(Long withdrawalId, WithdrawalProcessRequest request) {
        WithdrawalRequest wr = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new NotFoundException("Withdrawal request not found"));

        if (wr.getStatus() != WithdrawalStatus.PENDING) {
            throw new BadRequestException("Only PENDING withdrawal requests can be rejected");
        }

        // Hoàn lại balance
        CineWallet wallet = cineWalletRepository.findByUserForUpdate(wr.getUser())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        BigDecimal newBalance = wallet.getBalance().add(wr.getAmount());
        wallet.setBalance(newBalance);
        cineWalletRepository.save(wallet);

        wr.setStatus(WithdrawalStatus.REJECTED);
        wr.setProcessedNote(request.note());
        wr.setProcessedAt(LocalDateTime.now());
        withdrawalRequestRepository.save(wr);

        // Tạo wallet transaction WITHDRAWAL_REJECTED
        walletTransactionRepository.save(new WalletTransaction(
                wallet, wr.getUser(), null,
                WalletTransactionType.WITHDRAWAL_REJECTED,
                wr.getAmount(),
                newBalance,
                "WD-REJECTED-" + wr.getId(),
                "Từ chối rút tiền: " + (request.note() != null ? request.note() : "Không có ghi chú")
        ));

        log.info("Withdrawal {} rejected, {} VND returned to wallet for user {}",
                withdrawalId, wr.getAmount(), wr.getUser().getEmail());
        auditLogService.record(AuditActionType.REJECT, "WALLET", wr.getId(),
                wr.getUser().getEmail() + " - từ chối rút " + wr.getAmount() + " VND");
        return WithdrawalResponse.from(wr);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletDashboardResponse getDashboard() {
        BigDecimal totalBalance = cineWalletRepository.sumBalances();
        BigDecimal totalRefunded = walletTransactionRepository.sumAmountByType(WalletTransactionType.REFUND_CREDIT);
        BigDecimal totalWithdrawn = withdrawalRequestRepository.sumAmountByStatus(WithdrawalStatus.PAID);
        BigDecimal pendingAmount = withdrawalRequestRepository.sumAmountByStatus(WithdrawalStatus.PENDING);
        long pendingCount = withdrawalRequestRepository.countByStatus(WithdrawalStatus.PENDING);
        long totalWallets = cineWalletRepository.count();

        // Recent transactions (top 10)
        Pageable top10 = PageRequest.of(0, 10);
        List<WalletTransactionResponse> recentTx = walletTransactionRepository
                .findAllFiltered(top10)
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

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private CineWallet getOrCreateWallet(User user) {
        return cineWalletRepository.findByUser(user)
                .orElseGet(() -> cineWalletRepository.save(new CineWallet(user)));
    }
}
