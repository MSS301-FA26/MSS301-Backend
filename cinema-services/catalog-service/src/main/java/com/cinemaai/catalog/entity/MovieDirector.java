package com.cinemaai.catalog.entity;

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
        name = "movie_directors",
        indexes = {
                @Index(name = "idx_movie_directors_movie", columnList = "movie_id"),
                @Index(name = "idx_movie_directors_director_movie", columnList = "director_id, movie_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovieDirector extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "director_id", nullable = false)
    private Director director;

    public MovieDirector(Movie movie, Director director) {
        this.movie = movie;
        this.director = director;
    }
}
