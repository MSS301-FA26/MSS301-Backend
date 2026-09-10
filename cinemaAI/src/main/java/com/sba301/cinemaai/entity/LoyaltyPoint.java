package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.LoyaltyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per user — stores the user's current loyalty balance.
 * points      : currently usable points (can be redeemed)
 * totalPoints : cumulative points earned, never decreases (audit trail)
 */
@Getter
@Entity
@Table(name = "loyalty_points")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoyaltyPoint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter
    @Column(nullable = false)
    private int points = 0;

    @Setter
    @Column(name = "total_points", nullable = false)
    private int totalPoints = 0;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoyaltyStatus status = LoyaltyStatus.ACTIVE;

    public LoyaltyPoint(User user) {
        this.user = user;
        this.points = 0;
        this.totalPoints = 0;
        this.status = LoyaltyStatus.ACTIVE;
    }
}
