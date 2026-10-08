package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.ApprovalAction;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "movie_approval_histories",
        indexes = {
                @Index(name = "idx_mah_movie_id", columnList = "movie_id"),
                @Index(name = "idx_mah_actor_id", columnList = "actor_user_id"),
                @Index(name = "idx_mah_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovieApprovalHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalAction action;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", length = 30)
    private String toStatus;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    public MovieApprovalHistory(Movie movie, ApprovalAction action, String fromStatus, String toStatus, String comment, User actor) {
        this.movie = movie;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.comment = comment;
        this.actor = actor;
    }

    public MovieApprovalHistory(Movie movie, User actor, ApprovalAction action, Object fromStatus, Object toStatus, String comment) {
        this.movie = movie;
        this.actor = actor;
        this.action = action;
        this.fromStatus = fromStatus != null ? fromStatus.toString() : null;
        this.toStatus = toStatus != null ? toStatus.toString() : null;
        this.comment = comment;
    }
}
