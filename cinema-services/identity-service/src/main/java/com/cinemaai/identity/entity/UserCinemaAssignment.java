package com.cinemaai.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "user_cinema_assignments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCinemaAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Setter
    @Column(name = "cinema_id", nullable = false)
    private Long cinemaId;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id")
    private User assignedBy;

    public UserCinemaAssignment(User user, Long cinemaId, User assignedBy) {
        this.user = user;
        this.cinemaId = cinemaId;
        this.assignedBy = assignedBy;
    }
}
