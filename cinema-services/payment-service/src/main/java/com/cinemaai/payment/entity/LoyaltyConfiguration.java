package com.cinemaai.payment.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "loyalty_configurations")
public class LoyaltyConfiguration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cinema_id", unique = true)
    private Long cinemaId;

    @Column(name = "cinema_name")
    private String cinemaName;

    @Column(name = "earning_rate_percent", nullable = false)
    @Builder.Default
    private BigDecimal earningRatePercent = BigDecimal.valueOf(1.00);

    @Column(name = "redemption_rate_percent", nullable = false)
    @Builder.Default
    private BigDecimal redemptionRatePercent = BigDecimal.valueOf(100.00);

    @Column(name = "redemption_points", nullable = false)
    @Builder.Default
    private int redemptionPoints = 1000;

    @Column(name = "redemption_value_vnd", nullable = false)
    @Builder.Default
    private BigDecimal redemptionValueVnd = BigDecimal.valueOf(1000.00);

    @Column(name = "max_redemption_percent", nullable = false)
    @Builder.Default
    private BigDecimal maxRedemptionPercent = BigDecimal.valueOf(100.00);

    @Column(name = "expiry_month", nullable = false)
    @Builder.Default
    private int expiryMonth = 12;

    @Column(name = "expiry_day", nullable = false)
    @Builder.Default
    private int expiryDay = 31;

    @Column(name = "expiry_time", length = 8, nullable = false)
    @Builder.Default
    private String expiryTime = "23:59:59";

    @Column(name = "expiry_date", length = 20)
    private String expiryDate;

    @Column(name = "last_expired_at")
    private LocalDateTime lastExpiredAt;

    @Column(name = "last_reset_at")
    private LocalDateTime lastResetAt;

    @Column(name = "last_reset_source", length = 100)
    private String lastResetSource;
}