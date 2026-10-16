package com.cinemaai.payment.scheduler;

import com.cinemaai.payment.entity.Payment;
import com.cinemaai.payment.entity.PaymentTransaction;
import com.cinemaai.payment.enums.PaymentStatus;
import com.cinemaai.payment.enums.PaymentTransactionStatus;
import com.cinemaai.payment.repository.PaymentRepository;
import com.cinemaai.payment.repository.PaymentTransactionRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpirationScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Scheduled(fixedDelay = 60000) // check every 1 minute
    @Transactional
    public void expireStalePayments() {
        LocalDateTime now = LocalDateTime.now();
        List<Payment> stalePayments = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING && p.getExpiresAt() != null && p.getExpiresAt().isBefore(now))
                .toList();

        if (stalePayments.isEmpty()) {
            return;
        }

        log.info("Found {} stale pending payments to expire.", stalePayments.size());
        for (Payment payment : stalePayments) {
            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);

            List<PaymentTransaction> txs = paymentTransactionRepository.findByPaymentIdOrderByCreatedAtDesc(payment.getId());
            for (PaymentTransaction tx : txs) {
                if (tx.getStatus() == PaymentTransactionStatus.PENDING) {
                    tx.setStatus(PaymentTransactionStatus.EXPIRED);
                    tx.setFailureCode("PAYMENT_EXPIRED");
                    tx.setFailureMessage("Giao dịch thanh toán đã hết hạn thời gian giữ chỗ.");
                    tx.setCompletedAt(now);
                    paymentTransactionRepository.save(tx);
                }
            }
        }
    }
}
