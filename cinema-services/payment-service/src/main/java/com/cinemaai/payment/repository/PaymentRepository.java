package com.cinemaai.payment.repository;

import com.cinemaai.payment.entity.Payment;
import com.cinemaai.payment.enums.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    // Phase 1: Find active pending payment for Booking
    Optional<Payment> findByBookingIdAndStatus(
            Long bookingId,
            PaymentStatus status
    );

    Optional<Payment> findByFoodOrderId(Long foodOrderId);

    // Phase 1: Find active pending payment for Food Order
    Optional<Payment> findByFoodOrderIdAndStatus(
            Long foodOrderId,
            PaymentStatus status
    );

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
}