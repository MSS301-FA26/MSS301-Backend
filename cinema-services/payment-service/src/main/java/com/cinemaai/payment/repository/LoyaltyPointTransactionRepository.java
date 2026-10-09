package com.cinemaai.payment.repository;

import com.cinemaai.payment.entity.LoyaltyPointTransaction;
import com.cinemaai.payment.enums.LoyaltyPointType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LoyaltyPointTransactionRepository extends JpaRepository<LoyaltyPointTransaction, Long>, JpaSpecificationExecutor<LoyaltyPointTransaction> {
    List<LoyaltyPointTransaction> findByUserIdOrderByOccurredAtDesc(Long userId);
    Page<LoyaltyPointTransaction> findByUserIdOrderByOccurredAtDesc(Long userId, Pageable pageable);
    List<LoyaltyPointTransaction> findByBookingId(Long bookingId);

    @Query("SELECT COALESCE(SUM(t.pointsDelta), 0) FROM LoyaltyPointTransaction t WHERE t.bookingId = :bookingId AND t.type = :type")
    long sumDeltaByBookingIdAndType(@Param("bookingId") Long bookingId, @Param("type") LoyaltyPointType type);
}
