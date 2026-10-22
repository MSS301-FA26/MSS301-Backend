package com.cinemaai.payment.entity;

import com.cinemaai.payment.enums.LoyaltyPointType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyPointTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "booking_code")
    private String bookingCode;

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
}
