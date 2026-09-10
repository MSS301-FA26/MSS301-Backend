package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.LoyaltyPointTransaction;
import com.sba301.cinemaai.enums.LoyaltyPointType;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoyaltyPointTransactionRepository extends
        JpaRepository<LoyaltyPointTransaction, Long>,
        JpaSpecificationExecutor<LoyaltyPointTransaction> {

    @Query("""
            SELECT COALESCE(SUM(tx.pointsDelta), 0) FROM LoyaltyPointTransaction tx
            WHERE tx.type = :type
              AND tx.occurredAt >= :from
              AND tx.occurredAt <= :to
            """)
    long sumDeltaByTypeBetween(
            @Param("type") LoyaltyPointType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            SELECT COALESCE(SUM(tx.pointsDelta), 0) FROM LoyaltyPointTransaction tx
            WHERE tx.booking.id = :bookingId
              AND tx.type = :type
            """)
    long sumDeltaByBookingAndType(
            @Param("bookingId") Long bookingId,
            @Param("type") LoyaltyPointType type
    );
}
