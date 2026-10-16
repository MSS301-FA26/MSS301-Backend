package com.cinemaai.payment.repository;

import com.cinemaai.payment.entity.Refund;
import com.cinemaai.payment.enums.RefundStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByRefundCode(String refundCode);
    List<Refund> findByPaymentId(Long paymentId);
    List<Refund> findByBookingId(Long bookingId);
    Page<Refund> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Refund> findByStatusOrderByCreatedAtDesc(RefundStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.paymentId = :paymentId AND r.status = 'COMPLETED'")
    BigDecimal sumCompletedRefundsByPaymentId(@Param("paymentId") Long paymentId);
}
