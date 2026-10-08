package com.cinemaai.payment.repository;

import com.cinemaai.payment.entity.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByFoodOrderId(Long foodOrderId);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
