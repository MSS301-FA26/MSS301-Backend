package com.cinemaai.payment.service;

import com.cinemaai.payment.client.IdentityClient;
import com.cinemaai.payment.entity.LoyaltyPoint;
import com.cinemaai.payment.entity.LoyaltyPointTransaction;
import com.cinemaai.payment.enums.LoyaltyPointType;
import com.cinemaai.payment.enums.LoyaltyStatus;
import com.cinemaai.payment.mapper.LoyaltyMapper;
import com.cinemaai.payment.repository.LoyaltyConfigurationRepository;
import com.cinemaai.payment.repository.LoyaltyPointRepository;
import com.cinemaai.payment.repository.LoyaltyPointTransactionRepository;
import com.cinemaai.payment.service.impl.LoyaltyServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoyaltyExpirationBehaviorTest {
    @Test
    void expiresOnlyPositiveBalancesAndAppendsExpireLedgerEntries() {
        LoyaltyPointRepository points = mock(LoyaltyPointRepository.class);
        LoyaltyPointTransactionRepository transactions = mock(LoyaltyPointTransactionRepository.class);
        LoyaltyPoint positive = LoyaltyPoint.builder()
                .userId(1L).points(250).totalPoints(900).status(LoyaltyStatus.ACTIVE).build();
        LoyaltyPoint zero = LoyaltyPoint.builder()
                .userId(2L).points(0).totalPoints(400).status(LoyaltyStatus.ACTIVE).build();
        LoyaltyPoint suspendedWithBalance = LoyaltyPoint.builder()
                .userId(3L).points(50).totalPoints(100).status(LoyaltyStatus.SUSPENDED).build();
        when(points.findAll()).thenReturn(List.of(positive, zero, suspendedWithBalance));
        LoyaltyServiceImpl service = new LoyaltyServiceImpl(points, transactions,
                mock(LoyaltyConfigurationRepository.class), mock(IdentityClient.class), mock(LoyaltyMapper.class));

        int affected = service.expireAllActivePoints("ADMIN");

        assertEquals(2, affected);
        assertEquals(0, positive.getPoints());
        assertEquals(900, positive.getTotalPoints());
        assertEquals(0, zero.getPoints());
        assertEquals(0, suspendedWithBalance.getPoints());
        assertEquals(100, suspendedWithBalance.getTotalPoints());
        verify(points).save(positive);
        verify(points, never()).save(zero);
        verify(points).save(suspendedWithBalance);

        ArgumentCaptor<LoyaltyPointTransaction> ledger = ArgumentCaptor.forClass(LoyaltyPointTransaction.class);
        verify(transactions, org.mockito.Mockito.times(2)).save(ledger.capture());
        org.mockito.Mockito.verifyNoMoreInteractions(transactions);
        assertEquals(List.of(LoyaltyPointType.EXPIRE, LoyaltyPointType.EXPIRE),
                ledger.getAllValues().stream().map(LoyaltyPointTransaction::getType).toList());
        assertEquals(List.of(-250, -50),
                ledger.getAllValues().stream().map(LoyaltyPointTransaction::getPointsDelta).toList());
        ledger.getAllValues().forEach(entry -> {
            assertEquals(0, entry.getBalanceAfter());
            assertNotNull(entry.getOccurredAt());
        });
    }
}
