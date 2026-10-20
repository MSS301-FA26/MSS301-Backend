package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.AgeRating;
import com.sba301.cinemaai.enums.AgeRatingConverter;
import com.sba301.cinemaai.enums.MovieApprovalStatus;
import com.sba301.cinemaai.enums.MoviePublicationStatus;
import com.sba301.cinemaai.enums.MovieStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(
        name = "movies",
        indexes = {
                @Index(name = "idx_movies_status", columnList = "status"),
                @Index(name = "idx_movies_approval_status", columnList = "approval_status"),
                @Index(name = "idx_movies_publication_status", columnList = "publication_status"),
                @Index(name = "idx_movies_release_date", columnList = "release_date"),
                @Index(name = "idx_movies_status_release_id", columnList = "status, release_date, id"),
                @Index(name = "idx_movies_release_id", columnList = "release_date, id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Movie extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, unique = true)
    private String title;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String description;

    @Setter
    @Column(name = "trailer_url", length = 500)
    private String trailerUrl;

    @Setter
    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Setter
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Setter
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Setter
    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Setter
    @Column(name = "end_date")
    private LocalDate endDate;

    @Setter
    @Column(length = 50)
    private String language;

    @Setter
    @Column(name = "subtitle_language", length = 50)
    private String subtitleLanguage;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MovieStatus status = MovieStatus.UPCOMING;

    @Setter
    @Convert(converter = AgeRatingConverter.class)
    @Column(name = "age_rating", length = 20)
    private AgeRating ageRating;

    @Setter
    private String director;

    @Setter
    @Column(name = "main_actors", length = 1000)
    private String mainActors;

    @Setter
    @Column(name = "cast_list", length = 1000)
    private String castList;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 30)
    private MovieApprovalStatus approvalStatus = MovieApprovalStatus.DRAFT;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", length = 30)
    private MoviePublicationStatus publicationStatus = MoviePublicationStatus.UNPUBLISHED;

    @Setter
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    @Setter
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Setter
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by")
    private User rejectedBy;

    @Setter
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Setter
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public Movie(String title, int durationMinutes, MovieStatus status) {
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.approvalStatus = MovieApprovalStatus.DRAFT;
        this.publicationStatus = MoviePublicationStatus.UNPUBLISHED;
    }

    public Movie(String title, int durationMinutes, MovieStatus status, MovieApprovalStatus approvalStatus, MoviePublicationStatus publicationStatus) {
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.approvalStatus = approvalStatus;
        this.publicationStatus = publicationStatus;
    }
}
