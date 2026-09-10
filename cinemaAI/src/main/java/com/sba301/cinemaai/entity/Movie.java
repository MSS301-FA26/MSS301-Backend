package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.AgeRating;
import com.sba301.cinemaai.enums.AgeRatingConverter;
import com.sba301.cinemaai.enums.MovieStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
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
    @Column(name = "cast_list", columnDefinition = "TEXT")
    private String castList;

    public Movie(String title, int durationMinutes, MovieStatus status) {
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.status = status;
    }
}
