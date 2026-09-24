package com.cinemaai.payment.entity;

import com.cinemaai.payment.enums.WithdrawalStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@Entity
@Table(
        name = "withdrawal_requests",
        indexes = {
                @Index(name = "idx_withdrawals_user", columnList = "user_id"),
                @Index(name = "idx_withdrawals_status_created", columnList = "status, created_at")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private CineWallet wallet;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "account_holder", nullable = false, length = 120)
    private String accountHolder;

    @Column(name = "wallet_phone", length = 20)
    private String walletPhone;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Column(name = "processed_method", length = 50)
    private String processedMethod;

    @Column(name = "processed_note", length = 500)
    private String processedNote;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
