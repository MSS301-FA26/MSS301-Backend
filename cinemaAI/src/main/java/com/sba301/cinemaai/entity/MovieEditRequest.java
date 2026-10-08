package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.MovieEditRequestStatus;
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
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "movie_edit_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovieEditRequest extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private MovieEditRequestStatus status = MovieEditRequestStatus.PENDING;
    @Column(name = "proposed_data", nullable = false, columnDefinition = "TEXT")
    private String proposedData;
    @Column(columnDefinition = "TEXT")
    private String reason;
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    @Version private Long version;

    public MovieEditRequest(Movie movie, User requester, String proposedData, String reason) {
        this.movie = movie;
        this.requestedBy = requester;
        this.proposedData = proposedData;
        this.reason = reason;
    }

    public void approve(User reviewer) {
        this.status = MovieEditRequestStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(User reviewer, String rejectionReason) {
        this.status = MovieEditRequestStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = rejectionReason;
    }
}
