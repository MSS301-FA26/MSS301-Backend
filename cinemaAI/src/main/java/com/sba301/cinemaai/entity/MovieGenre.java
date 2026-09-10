package com.sba301.cinemaai.entity;

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
        name = "movie_genres",
        indexes = {
                @Index(name = "idx_movie_genres_movie", columnList = "movie_id"),
                @Index(name = "idx_movie_genres_genre_movie", columnList = "genre_id, movie_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovieGenre extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    public MovieGenre(Movie movie, Genre genre) {
        this.movie = movie;
        this.genre = genre;
    }
}
