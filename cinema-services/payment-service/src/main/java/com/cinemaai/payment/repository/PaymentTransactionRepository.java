package com.cinemaai.payment.repository;

import com.cinemaai.payment.entity.PaymentTransaction;
import com.cinemaai.payment.enums.PaymentTransactionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);
    Optional<PaymentTransaction> findByProviderReference(String providerReference);
    Optional<PaymentTransaction> findByProviderTransactionId(String providerTransactionId);
    List<PaymentTransaction> findByStatusAndExpiresAtBefore(PaymentTransactionStatus status, LocalDateTime threshold);
}
