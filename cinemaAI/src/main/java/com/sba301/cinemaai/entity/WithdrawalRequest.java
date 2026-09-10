package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.WithdrawalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(
        name = "withdrawal_requests",
        indexes = {
                @Index(name = "idx_withdrawals_user", columnList = "user_id"),
                @Index(name = "idx_withdrawals_status_created", columnList = "status, created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawalRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private CineWallet wallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Setter
    @Column(name = "processed_method", length = 50)
    private String processedMethod;

    @Setter
    @Column(name = "processed_note", length = 500)
    private String processedNote;

    @Setter
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public WithdrawalRequest(
            CineWallet wallet,
            User user,
            BigDecimal amount,
            String bankName,
            String accountNumber,
            String accountHolder,
            String walletPhone
    ) {
        this.wallet = wallet;
        this.user = user;
        this.amount = amount;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.walletPhone = walletPhone;
    }
}
