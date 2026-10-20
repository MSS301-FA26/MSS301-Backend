package com.cinemaai.catalog.entity;

import com.cinemaai.catalog.enums.ReviewReportReason;
import com.cinemaai.catalog.enums.ReviewReportStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(
        name = "review_reports",
        indexes = {
                @Index(name = "idx_review_reports_review", columnList = "review_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "reporter_user_id", nullable = false)
    private Long reporterUserId;

    @Column(name = "reporter_email")
    private String reporterEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReviewReportReason reason;

    @Column(length = 1000)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewReportStatus status = ReviewReportStatus.PENDING;
}
