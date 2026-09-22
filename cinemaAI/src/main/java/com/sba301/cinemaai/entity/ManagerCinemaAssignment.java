package com.sba301.cinemaai.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "manager_cinema_assignments", uniqueConstraints =
        @UniqueConstraint(name = "uq_manager_cinema", columnNames = {"user_id", "cinema_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManagerCinemaAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    public ManagerCinemaAssignment(User user, Cinema cinema) {
        this.user = user;
        this.cinema = cinema;
    }
}
