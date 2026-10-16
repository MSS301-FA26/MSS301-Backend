package com.cinemaai.payment.service;

import com.cinemaai.payment.dto.request.CreditWalletInternalRequest;
import com.cinemaai.payment.dto.request.WithdrawalCreateRequest;
import com.cinemaai.payment.dto.request.WithdrawalProcessRequest;
import com.cinemaai.payment.dto.response.WalletResponse;
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
import com.cinemaai.payment.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private CineWalletRepository cineWalletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Mock
    private PayoutProvider payoutProvider;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    @DisplayName("Get Wallet - Returns existing wallet")
    void testGetWallet_Existing() {
        Long userId = 1L;
        CineWallet wallet = CineWallet.builder()
                .id(10L)
                .userId(userId)
                .balance(BigDecimal.valueOf(500_000))
                .build();
        ReflectionTestUtils.setField(wallet, "createdAt", LocalDateTime.now());
        when(cineWalletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getWallet(userId);
        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(500_000), response.balance());
    }

    @Test
    @DisplayName("Create Withdrawal - Insufficient balance throws BadRequestException")
    void testCreateWithdrawal_InsufficientBalance() {
        Long userId = 1L;
        CineWallet wallet = CineWallet.builder()
                .id(10L)
                .userId(userId)
                .balance(BigDecimal.valueOf(500_00))
                .build();
        when(cineWalletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        WithdrawalCreateRequest request = new WithdrawalCreateRequest(
                BigDecimal.valueOf(100_000), "VCB", "0123456789", "NGUYEN VAN A", null);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                walletService.createWithdrawalRequest(userId, request));
        assertTrue(ex.getMessage().contains("không đủ"));
    }

    @Test
    @DisplayName("Create Withdrawal - Successful payout marks COMPLETED and records WITHDRAWAL_DEBIT")
    void testCreateWithdrawal_SuccessPayout() {
        Long userId = 1L;
        CineWallet wallet = CineWallet.builder()
                .id(10L)
                .userId(userId)
                .balance(BigDecimal.valueOf(200_000))
                .build();
        when(cineWalletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenAnswer(invocation -> {
            WithdrawalRequest wr = invocation.getArgument(0);
            wr.setId(99L);
            return wr;
        });

        when(payoutProvider.processPayout(any(WithdrawalRequest.class))).thenReturn(
                PayoutResult.success("TX-PAYOUT-999"));

        WithdrawalCreateRequest request = new WithdrawalCreateRequest(
                BigDecimal.valueOf(100_000), "VCB", "0123456789", "NGUYEN VAN A", null);

        WithdrawalResponse response = walletService.createWithdrawalRequest(userId, request);

        assertNotNull(response);
        assertEquals(WithdrawalStatus.COMPLETED, response.status());
        assertEquals(BigDecimal.valueOf(100_000), wallet.getBalance());

        // Verify debit transaction recorded
        verify(walletTransactionRepository).save(argThat(tx ->
                tx.getType() == WalletTransactionType.WITHDRAWAL_DEBIT
                        && tx.getAmount().compareTo(BigDecimal.valueOf(-100_000)) == 0
        ));
    }

    @Test
    @DisplayName("Create Withdrawal - Failed payout reverses balance back to wallet")
    void testCreateWithdrawal_FailedPayoutReversesBalance() {
        Long userId = 1L;
        CineWallet wallet = CineWallet.builder()
                .id(10L)
                .userId(userId)
                .balance(BigDecimal.valueOf(200_000))
                .build();
        when(cineWalletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        when(withdrawalRequestRepository.save(any(WithdrawalRequest.class))).thenAnswer(invocation -> {
            WithdrawalRequest wr = invocation.getArgument(0);
            wr.setId(99L);
            return wr;
        });

        when(payoutProvider.processPayout(any(WithdrawalRequest.class))).thenReturn(
                PayoutResult.failure("REJECTED_BY_BANK", "Tài khoản thụ hưởng không hợp lệ"));

        WithdrawalCreateRequest request = new WithdrawalCreateRequest(
                BigDecimal.valueOf(100_000), "VCB", "9999999999", "NGUYEN VAN A", null);

        WithdrawalResponse response = walletService.createWithdrawalRequest(userId, request);

        assertNotNull(response);
        assertEquals(WithdrawalStatus.FAILED, response.status());
        // Verify balance was restored back to 200_000
        assertEquals(BigDecimal.valueOf(200_000), wallet.getBalance());

        // Verify reversal transaction recorded
        verify(walletTransactionRepository).save(argThat(tx ->
                tx.getType() == WalletTransactionType.WITHDRAWAL_REVERSAL
                        && tx.getAmount().compareTo(BigDecimal.valueOf(100_000)) == 0
        ));
    }

    @Test
    @DisplayName("Credit Internal - Adds funds and records transaction")
    void testCreditWalletInternal() {
        Long userId = 1L;
        CineWallet wallet = CineWallet.builder()
                .id(10L)
                .userId(userId)
                .balance(BigDecimal.valueOf(50_000))
                .build();
        when(cineWalletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));

        CreditWalletInternalRequest req = new CreditWalletInternalRequest(
                userId, BigDecimal.valueOf(75_000), 101L, "BK-101", "Bonus credits");

        WalletResponse resp = walletService.creditWalletInternal(req);

        assertNotNull(resp);
        assertEquals(BigDecimal.valueOf(125_000), wallet.getBalance());
        verify(walletTransactionRepository).save(argThat(tx ->
                tx.getAmount().compareTo(BigDecimal.valueOf(75_000)) == 0
                        && "REFUND".equals(tx.getReferenceType())
        ));
    }
}
