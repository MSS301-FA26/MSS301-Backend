package com.cinemaai.payment.service;

import com.cinemaai.payment.client.IdentityClient;
import com.cinemaai.payment.dto.request.AwardBookingPointsRequest;
import com.cinemaai.payment.dto.request.RefundBookingPointsRequest;
import com.cinemaai.payment.entity.LoyaltyPoint;
import com.cinemaai.payment.entity.LoyaltyPointTransaction;
import com.cinemaai.payment.enums.LoyaltyPointType;
import com.cinemaai.payment.enums.LoyaltyStatus;
import com.cinemaai.payment.mapper.LoyaltyMapper;
import com.cinemaai.payment.repository.LoyaltyConfigurationRepository;
import com.cinemaai.payment.repository.LoyaltyPointRepository;
import com.cinemaai.payment.repository.LoyaltyPointTransactionRepository;
import com.cinemaai.payment.service.impl.LoyaltyServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoyaltyRedemptionSecurityTest {
    private LoyaltyPointRepository points;
    private LoyaltyPointTransactionRepository transactions;
    private LoyaltyServiceImpl service;
    private LoyaltyPoint balance;

    @BeforeEach
    void setUp() {
        points = mock(LoyaltyPointRepository.class);
        transactions = mock(LoyaltyPointTransactionRepository.class);
        balance = LoyaltyPoint.builder()
                .userId(7L).points(0).totalPoints(0).status(LoyaltyStatus.ACTIVE).build();
        when(points.findByUserId(7L)).thenReturn(Optional.of(balance));
        service = new LoyaltyServiceImpl(points, transactions, mock(LoyaltyConfigurationRepository.class),
                mock(IdentityClient.class), mock(LoyaltyMapper.class));
    }

    @Test
    void unverifiedRedeemedPointsDoNotCreateRedeemLedgerButEarningStillWorks() {
        when(transactions.sumDeltaByBookingIdAndType(99L, LoyaltyPointType.EARN)).thenReturn(0L);

        service.awardPointsForBooking(new AwardBookingPointsRequest(
                7L, 99L, "BK99", new BigDecimal("10000"), 1_000_000, null));

        assertEquals(100, balance.getPoints());
        assertEquals(100, balance.getTotalPoints());
        ArgumentCaptor<LoyaltyPointTransaction> captor = ArgumentCaptor.forClass(LoyaltyPointTransaction.class);
        verify(transactions).save(captor.capture());
        assertEquals(LoyaltyPointType.EARN, captor.getValue().getType());
        assertEquals(100, captor.getValue().getPointsDelta());
    }

    @Test
    void duplicateEarnDoesNotAwardAgain() {
        when(transactions.sumDeltaByBookingIdAndType(99L, LoyaltyPointType.EARN)).thenReturn(100L);

        service.awardPointsForBooking(new AwardBookingPointsRequest(
                7L, 99L, "BK99", new BigDecimal("10000"), 0, null));

        assertEquals(0, balance.getPoints());
        verify(transactions, never()).save(any());
    }

    @Test
    void refundDoesNotRestorePointsWithoutCommittedRedeemLedger() {
        when(transactions.sumDeltaByBookingIdAndType(99L, LoyaltyPointType.RESTORE)).thenReturn(0L);
        when(transactions.sumDeltaByBookingIdAndType(99L, LoyaltyPointType.REDEEM)).thenReturn(0L);
        when(transactions.sumDeltaByBookingIdAndType(99L, LoyaltyPointType.REVOKE)).thenReturn(0L);
        when(transactions.sumDeltaByBookingIdAndType(99L, LoyaltyPointType.EARN)).thenReturn(0L);

        service.refundPointsForBooking(new RefundBookingPointsRequest(
                7L, 99L, "BK99", BigDecimal.ZERO, 1_000_000, "refund"));

        assertEquals(0, balance.getPoints());
        verify(transactions, never()).save(any());
    }
}
