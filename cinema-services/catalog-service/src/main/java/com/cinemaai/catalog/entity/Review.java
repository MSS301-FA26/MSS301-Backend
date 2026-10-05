package com.cinemaai.catalog.entity;

import com.cinemaai.catalog.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "reviews",
        indexes = {
                @Index(name = "idx_reviews_movie_status", columnList = "movie_id, status"),
                @Index(name = "idx_reviews_status_created", columnList = "status, created_at"),
                @Index(name = "idx_reviews_user", columnList = "user_id"),
                @Index(name = "idx_reviews_booking", columnList = "booking_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "user_full_name")
    private String userFullName;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 2000)
    private String content;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewStatus status = ReviewStatus.PUBLISHED;

    @Builder.Default
    @Column(name = "verified_booking", nullable = false)
    private boolean verifiedBooking = false;

    @Builder.Default
    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;

    @Builder.Default
    @Column(name = "contains_spoiler", nullable = false)
    private boolean containsSpoiler = false;

    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    @Column(name = "hidden_by")
    private String hiddenBy;

    @Column(name = "moderation_reason", length = 500)
    private String moderationReason;
}
