package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.LoyaltyPointType;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "loyalty_point_transactions",
        indexes = {
                @Index(name = "idx_loyalty_tx_user", columnList = "user_id"),
                @Index(name = "idx_loyalty_tx_booking", columnList = "booking_id"),
                @Index(name = "idx_loyalty_tx_type", columnList = "type"),
                @Index(name = "idx_loyalty_tx_occurred_at", columnList = "occurred_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoyaltyPointTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoyaltyPointType type;

    @Column(name = "points_delta", nullable = false)
    private int pointsDelta;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Column(length = 500)
    private String note;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public LoyaltyPointTransaction(
            User user,
            Booking booking,
            LoyaltyPointType type,
            int pointsDelta,
            int balanceAfter,
            String note
    ) {
        this.user = user;
        this.booking = booking;
        this.type = type;
        this.pointsDelta = pointsDelta;
        this.balanceAfter = balanceAfter;
        this.note = note;
        this.occurredAt = LocalDateTime.now();
    }
}
