package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.enums.ShowtimeStatus;
import com.sba301.cinemaai.enums.TicketType;
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
        name = "showtimes",
        indexes = {
                @Index(name = "idx_showtimes_movie", columnList = "movie_id"),
                @Index(name = "idx_showtimes_room", columnList = "room_id"),
                @Index(name = "idx_showtimes_start_time", columnList = "start_time")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Showtime extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "start_time", nullable = false)
    @Setter
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    @Setter
    private LocalDateTime endTime;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    @Setter
    private BigDecimal basePrice;

    @Column(name = "vip_price", precision = 12, scale = 2)
    @Setter
    private BigDecimal vipPrice;

    @Column(name = "couple_price", precision = 12, scale = 2)
    @Setter
    private BigDecimal couplePrice;

    @Column(name = "adult_standard_price", precision = 12, scale = 2)
    @Setter
    private BigDecimal adultStandardPrice;

    @Column(name = "child_standard_price", precision = 12, scale = 2)
    @Setter
    private BigDecimal childStandardPrice;

    @Column(name = "student_standard_price", precision = 12, scale = 2)
    @Setter
    private BigDecimal studentStandardPrice;

    @Column(name = "adult_vip_price", precision = 12, scale = 2)
    @Setter
    private BigDecimal adultVipPrice;

    @Column(name = "child_vip_price", precision = 12, scale = 2)
    @Setter
    private BigDecimal childVipPrice;

    @Column(name = "student_vip_price", precision = 12, scale = 2)
    @Setter
    private BigDecimal studentVipPrice;

    @Column(name = "adult_couple_price", precision = 12, scale = 2)
    @Setter
    private BigDecimal adultCouplePrice;

    @Column(name = "child_couple_price", precision = 12, scale = 2)
    @Setter
    private BigDecimal childCouplePrice;

    @Column(name = "student_couple_price", precision = 12, scale = 2)
    @Setter
    private BigDecimal studentCouplePrice;

    @Column(name = "weekend_surcharge", nullable = false)
    @Setter
    private boolean weekendSurcharge;

    @Column(name = "holiday_surcharge", nullable = false)
    @Setter
    private boolean holidaySurcharge;

    @Column(name = "late_night_surcharge_amount", nullable = false, precision = 12, scale = 2)
    @Setter
    private BigDecimal lateNightSurchargeAmount = BigDecimal.valueOf(20_000);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Setter
    private ShowtimeStatus status = ShowtimeStatus.SCHEDULED;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    @Setter
    private String cancellationReason;

    @Column(name = "cancelled_at")
    @Setter
    private LocalDateTime cancelledAt;

    public Showtime(Movie movie, Room room, LocalDateTime startTime, LocalDateTime endTime, BigDecimal basePrice) {
        this.movie = movie;
        this.room = room;
        this.startTime = startTime;
        this.endTime = endTime;
        this.basePrice = basePrice;
    }

    public BigDecimal getPriceForSeatType(SeatType seatType) {
        return getPriceForTicketAndSeatType(TicketType.ADULT, seatType);
    }

    public BigDecimal getPriceForTicketAndSeatType(TicketType ticketType, SeatType seatType) {
        BigDecimal configured = switch (ticketType) {
            case CHILD -> switch (normalizeSeatType(seatType)) {
                case VIP -> childVipPrice;
                case COUPLE -> childCouplePrice;
                default -> childStandardPrice;
            };
            case STUDENT -> switch (normalizeSeatType(seatType)) {
                case VIP -> studentVipPrice;
                case COUPLE -> studentCouplePrice;
                default -> studentStandardPrice;
            };
            default -> switch (normalizeSeatType(seatType)) {
                case VIP -> adultVipPrice;
                case COUPLE -> adultCouplePrice;
                default -> adultStandardPrice;
            };
        };
        BigDecimal fallback = switch (normalizeSeatType(seatType)) {
            case VIP    -> vipPrice    != null ? vipPrice    : basePrice.multiply(new BigDecimal("1.5"));
            case COUPLE -> couplePrice != null ? couplePrice : basePrice.multiply(new BigDecimal("2.0"));
            default     -> basePrice;
        };
        return defaultMoney(configured, fallback).add(getSurchargeAmount());
    }

    public BigDecimal getSurchargeAmount() {
        BigDecimal surcharge = BigDecimal.ZERO;
        if (weekendSurcharge) {
            surcharge = surcharge.add(BigDecimal.valueOf(10_000));
        }
        if (holidaySurcharge) {
            surcharge = surcharge.add(BigDecimal.valueOf(10_000));
        }
        if (isLateNight()) {
            surcharge = surcharge.add(defaultMoney(lateNightSurchargeAmount, BigDecimal.valueOf(20_000)));
        }
        return surcharge;
    }

    private boolean isLateNight() {
        int hour = startTime.getHour();
        return hour >= 23 || hour < 5;
    }

    private SeatType normalizeSeatType(SeatType seatType) {
        return seatType == SeatType.NORMAL ? SeatType.STANDARD : seatType;
    }

    private BigDecimal defaultMoney(BigDecimal value, BigDecimal fallback) {
        return value != null ? value : fallback;
    }
}
