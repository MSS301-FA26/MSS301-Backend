package com.sba301.cinemaai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Getter
@Entity
@Table(
        name = "movie_actors",
        indexes = {
                @Index(name = "idx_movie_actors_movie", columnList = "movie_id"),
                @Index(name = "idx_movie_actors_actor_movie", columnList = "actor_id, movie_id"),
                @Index(name = "idx_movie_actors_movie_main", columnList = "movie_id, is_main_actor")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovieActor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private Actor actor;

    @Column(name = "is_main_actor", nullable = false)
    private boolean mainActor;

    public MovieActor(Movie movie, Actor actor, boolean mainActor) {
        this.movie = movie;
        this.actor = actor;
        this.mainActor = mainActor;
    }
}
