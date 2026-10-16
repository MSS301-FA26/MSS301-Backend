package com.cinemaai.payment.entity;

import com.cinemaai.payment.enums.WalletTransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "wallet_transactions")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private CineWallet wallet;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WalletTransactionType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", precision = 12, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @Column(length = 255)
    private String description;
}
