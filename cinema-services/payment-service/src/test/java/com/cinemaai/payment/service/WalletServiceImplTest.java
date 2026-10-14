package com.cinemaai.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinemaai.payment.entity.CineWallet;
import com.cinemaai.payment.exception.NotFoundException;
import com.cinemaai.payment.repository.CineWalletRepository;
import com.cinemaai.payment.repository.WalletTransactionRepository;
import com.cinemaai.payment.repository.WithdrawalRequestRepository;
import com.cinemaai.payment.service.impl.WalletServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock private CineWalletRepository wallets;
    @Mock private WalletTransactionRepository transactions;
    @Mock private WithdrawalRequestRepository withdrawals;
    private WalletServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WalletServiceImpl(wallets, transactions, withdrawals);
    }

    @Test
    void missingWalletIsADomainErrorWithoutAnyInsert() {
        when(wallets.findByUserId(41L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getWallet(41L));

        verify(wallets, never()).save(any(CineWallet.class));
        verify(wallets, never()).saveAndFlush(any(CineWallet.class));
    }

    @Test
    void existingWalletReturnsItsActualIdAndBalanceWithoutWriting() {
        CineWallet wallet = CineWallet.builder().id(9L).userId(41L)
                .balance(new BigDecimal("75000.00")).build();
        when(wallets.findByUserId(41L)).thenReturn(Optional.of(wallet));

        var response = service.getWallet(41L);

        assertEquals(9L, response.id());
        assertEquals(new BigDecimal("75000.00"), response.balance());
        verify(wallets, never()).save(any(CineWallet.class));
    }

    @Test
    void getWalletTransactionRemainsReadOnly() throws Exception {
        Transactional transaction = WalletServiceImpl.class.getMethod("getWallet", Long.class)
                .getAnnotation(Transactional.class);
        assertTrue(transaction.readOnly());
    }
}
