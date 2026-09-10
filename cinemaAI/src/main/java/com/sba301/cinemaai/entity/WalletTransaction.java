package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.WalletTransactionType;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "wallet_transactions",
        indexes = {
                @Index(name = "idx_wallet_tx_wallet_created", columnList = "wallet_id, created_at"),
                @Index(name = "idx_wallet_tx_booking", columnList = "booking_id"),
                @Index(name = "idx_wallet_tx_type", columnList = "type")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private CineWallet wallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WalletTransactionType type;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 14, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @Column(length = 500)
    private String note;

    public WalletTransaction(
            CineWallet wallet,
            User user,
            Booking booking,
            WalletTransactionType type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String referenceCode,
            String note
    ) {
        this.wallet = wallet;
        this.user = user;
        this.booking = booking;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.referenceCode = referenceCode;
        this.note = note;
    }
}
