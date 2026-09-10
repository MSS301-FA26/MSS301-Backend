package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.BookingStatus;
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
        name = "bookings",
        indexes = {
                @Index(name = "idx_bookings_user", columnList = "user_id"),
                @Index(name = "idx_bookings_showtime", columnList = "showtime_id"),
                @Index(name = "idx_bookings_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_code", nullable = false, unique = true, length = 50)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @Setter
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Setter
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Setter
    @Column(name = "loyalty_points_redeemed", nullable = false)
    private int loyaltyPointsRedeemed = 0;

    @Setter
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status = BookingStatus.HOLDING;

    @Setter
    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Setter
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Setter
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Setter
    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Setter
    @Column(name = "refund_requested_at")
    private LocalDateTime refundRequestedAt;

    @Setter
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Setter
    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Setter
    @Column(name = "refund_method", length = 50)
    private String refundMethod;

    @Setter
    @Column(name = "bulk_refund", nullable = false)
    private boolean bulkRefund = false;

    @Setter
    @Column(name = "refund_retry_attempts", nullable = false)
    private int refundRetryAttempts = 0;

    @Setter
    @Column(name = "last_refund_attempt_at")
    private LocalDateTime lastRefundAttemptAt;

    @Setter
    @Column(name = "qr_code", length = 500)
    private String qrCode;

    public Booking(String bookingCode, User user, Showtime showtime, LocalDateTime holdExpiresAt) {
        this.bookingCode = bookingCode;
        this.user = user;
        this.showtime = showtime;
        this.holdExpiresAt = holdExpiresAt;
    }
}
