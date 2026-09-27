package com.cinemaai.payment.service.impl;

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

        // Deduct balance (hold for withdrawal)
        wallet.setBalance(wallet.getBalance().subtract(amount));
        cineWalletRepository.save(wallet);

        // Record holding transaction
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .userId(userId)
                .type(WalletTransactionType.WITHDRAWAL_HOLD)
                .amount(amount.negate())
                .balanceAfter(wallet.getBalance())
                .description("Yêu cầu rút tiền về ngân hàng " + request.bankName())
                .build();
        walletTransactionRepository.save(tx);

        WithdrawalRequest wr = WithdrawalRequest.builder()
                .wallet(wallet)
                .userId(userId)
                .amount(amount)
                .bankName(request.bankName())
                .accountNumber(request.accountNumber())
                .accountHolder(request.accountHolder())
                .walletPhone(request.walletPhone())
                .status(WithdrawalStatus.PENDING)
                .build();
        WithdrawalRequest saved = withdrawalRequestRepository.save(wr);
        log.info("Created withdrawal request #{} for userId: {}, amount: {}", saved.getId(), userId, amount);
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

        if (wr.getStatus() != WithdrawalStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể duyệt yêu cầu rút tiền ở trạng thái PENDING. Trạng thái hiện tại: " + wr.getStatus());
        }

        wr.setStatus(WithdrawalStatus.PAID);
        wr.setProcessedMethod(request != null && request.method() != null ? request.method() : "BANK_TRANSFER");
        wr.setProcessedNote(request != null ? request.note() : null);
        wr.setProcessedAt(LocalDateTime.now());
        withdrawalRequestRepository.save(wr);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wr.getWallet())
                .userId(wr.getUserId())
                .type(WalletTransactionType.WITHDRAWAL_PAID)
                .amount(BigDecimal.ZERO)
                .balanceAfter(wr.getWallet().getBalance())
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

        if (wr.getStatus() != WithdrawalStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể từ chối yêu cầu rút tiền ở trạng thái PENDING. Trạng thái hiện tại: " + wr.getStatus());
        }

        wr.setStatus(WithdrawalStatus.REJECTED);
        wr.setProcessedNote(request != null ? request.note() : "Bị từ chối bởi staff/admin");
        wr.setProcessedAt(LocalDateTime.now());
        withdrawalRequestRepository.save(wr);

        // Refund held money back to user's wallet
        CineWallet wallet = wr.getWallet();
        wallet.setBalance(wallet.getBalance().add(wr.getAmount()));
        cineWalletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .userId(wr.getUserId())
                .type(WalletTransactionType.WITHDRAWAL_REFUND)
                .amount(wr.getAmount())
                .balanceAfter(wallet.getBalance())
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
        BigDecimal totalWithdrawn = withdrawalRequestRepository.sumAmountByStatus(WithdrawalStatus.PAID);
        BigDecimal pendingAmount = withdrawalRequestRepository.sumAmountByStatus(WithdrawalStatus.PENDING);
        long pendingCount = withdrawalRequestRepository.countByStatus(WithdrawalStatus.PENDING);
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

    private CineWallet getOrCreateWallet(Long userId) {
        return cineWalletRepository.findByUserId(userId)
                .orElseGet(() -> cineWalletRepository.save(CineWallet.builder()
                        .userId(userId)
                        .balance(BigDecimal.ZERO)
                        .build()));
    }
}
