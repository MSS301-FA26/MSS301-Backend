package com.cinemaai.payment.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.cinemaai.payment.repository.CineWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class WalletProvisioningServiceTest {

    @Mock private CineWalletRepository wallets;
    private WalletProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new WalletProvisioningService(wallets);
    }

    @Test
    void newCustomerUsesAtomicInsertAndReportsCreated() {
        when(wallets.insertIfAbsent(41L)).thenReturn(1);

        assertTrue(service.ensureWalletExists(41L));

        verify(wallets).insertIfAbsent(41L);
        verifyNoMoreInteractions(wallets);
    }

    @Test
    void duplicateCustomerIsSuccessfulWithoutAnUpdateOrBalanceReset() {
        when(wallets.insertIfAbsent(41L)).thenReturn(0);

        assertFalse(service.ensureWalletExists(41L));

        verify(wallets).insertIfAbsent(41L);
        verifyNoMoreInteractions(wallets);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void invalidInternalUserIdsNeverReachTheDatabase(Long userId) {
        assertThrows(IllegalArgumentException.class, () -> service.ensureWalletExists(userId));
        verifyNoInteractions(wallets);
    }

    @Test
    void temporaryDatabaseFailurePropagatesForInfrastructureRetry() {
        var failure = new DataAccessResourceFailureException("test database unavailable");
        when(wallets.insertIfAbsent(41L)).thenThrow(failure);

        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> service.ensureWalletExists(41L)));
    }

    @Test
    void provisioningUsesAWritableTransaction() throws Exception {
        Transactional transaction = WalletProvisioningService.class.getMethod("ensureWalletExists", Long.class)
                .getAnnotation(Transactional.class);
        assertFalse(transaction.readOnly());
    }
}
