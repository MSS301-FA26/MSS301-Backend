package com.cinemaai.payment.entity;

import com.cinemaai.payment.enums.RefundStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@Entity
@Table(
        name = "refunds",
        indexes = {
                @Index(name = "idx_refunds_payment", columnList = "payment_id"),
                @Index(name = "idx_refunds_booking", columnList = "booking_id"),
                @Index(name = "idx_refunds_user", columnList = "user_id"),
                @Index(name = "idx_refunds_batch", columnList = "refund_batch_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_code", nullable = false, unique = true, length = 60)
    private String refundCode;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "food_order_id")
    private Long foodOrderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "remaining_refundable_amount", precision = 12, scale = 2)
    private BigDecimal remainingRefundableAmount;

    @Column(nullable = false, length = 255)
    private String reason;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundStatus status = RefundStatus.REQUESTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_batch_id")
    private RefundBatch refundBatch;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Builder.Default
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;
}
