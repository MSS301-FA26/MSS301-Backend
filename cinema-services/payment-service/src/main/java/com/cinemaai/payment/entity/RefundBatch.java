package com.cinemaai.payment.entity;

import com.cinemaai.payment.enums.RefundBatchStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Entity
@Table(
        name = "refund_batches",
        indexes = {
                @Index(name = "idx_refund_batches_showtime", columnList = "showtime_id"),
                @Index(name = "idx_refund_batches_cinema", columnList = "cinema_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundBatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_code", nullable = false, unique = true, length = 60)
    private String batchCode;

    @Column(name = "cinema_id")
    private Long cinemaId;

    @Column(name = "showtime_id")
    private Long showtimeId;

    @Column(nullable = false, length = 255)
    private String reason;

    @Builder.Default
    @Column(name = "total_bookings", nullable = false)
    private Integer totalBookings = 0;

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundBatchStatus status = RefundBatchStatus.REQUESTED;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "refundBatch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Refund> refunds = new ArrayList<>();
}
