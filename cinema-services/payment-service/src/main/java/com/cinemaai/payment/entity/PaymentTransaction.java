package com.cinemaai.payment.entity;

import com.cinemaai.payment.enums.PaymentProvider;
import com.cinemaai.payment.enums.PaymentTransactionStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@Entity
@Table(
        name = "payment_transactions",
        indexes = {
                @Index(name = "idx_payment_transactions_payment", columnList = "payment_id"),
                @Index(name = "idx_payment_transactions_provider_ref", columnList = "provider_reference"),
                @Index(name = "idx_payment_transactions_provider_tx", columnList = "provider_transaction_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProvider provider;

    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Column(name = "provider_reference", length = 120)
    private String providerReference;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String currency = "VND";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentTransactionStatus status = PaymentTransactionStatus.PENDING;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
