package com.sba301.cinemaai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "loyalty_configurations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoyaltyConfiguration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "earning_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal earningRatePercent = BigDecimal.valueOf(1);

    @Setter
    @Column(name = "redemption_points", nullable = false)
    private int redemptionPoints = 1000;

    @Setter
    @Column(name = "redemption_value_vnd", nullable = false, precision = 12, scale = 2)
    private BigDecimal redemptionValueVnd = BigDecimal.valueOf(1000);

    @Setter
    @Column(name = "expiry_month", nullable = false)
    private int expiryMonth = 12;

    @Setter
    @Column(name = "expiry_day", nullable = false)
    private int expiryDay = 31;

    @Setter
    @Column(name = "expiry_time", nullable = false, length = 8)
    private String expiryTime = "23:59:59";

    @Setter
    @Column(name = "last_expired_at")
    private LocalDate lastExpiredAt;

    @Setter
    @Column(name = "last_reset_at")
    private LocalDateTime lastResetAt;

    @Setter
    @Column(name = "last_reset_source", length = 20)
    private String lastResetSource;

    public LoyaltyConfiguration(boolean defaults) {
        this();
    }
}
