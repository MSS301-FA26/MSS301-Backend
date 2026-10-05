package com.cinemaai.catalog.service.impl;

import com.cinemaai.catalog.dto.response.banner.HotMovieSuggestionResponse;
import com.cinemaai.catalog.entity.HeroBanner;
import com.cinemaai.catalog.entity.Movie;
import com.cinemaai.catalog.entity.Showtime;
import com.cinemaai.catalog.enums.MovieStatus;
import com.cinemaai.catalog.repository.HeroBannerRepository;
import com.cinemaai.catalog.repository.MovieRepository;
import com.cinemaai.catalog.repository.ShowtimeRepository;
import com.cinemaai.catalog.service.HotMovieService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotMovieServiceImpl implements HotMovieService {

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final HeroBannerRepository heroBannerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<HotMovieSuggestionResponse> getHotMovieSuggestions(int limit) {
        int maxResults = limit > 0 ? Math.min(limit, 20) : 6;

        List<Movie> candidateMovies = movieRepository.findAll().stream()
                .filter(m -> m.getStatus() != MovieStatus.INACTIVE && m.getStatus() != MovieStatus.ENDED)
                .toList();

        if (candidateMovies.isEmpty()) {
            return List.of();
        }

        List<HotMovieCandidate> evaluatedList = new ArrayList<>();

        for (Movie movie : candidateMovies) {
            List<Showtime> showtimes = showtimeRepository.findByMovie(movie);
            int showtimeCount = showtimes.size();

            // Check if already placed on hero
            List<HeroBanner> existingHeroBanners = heroBannerRepository.findByLinkedMovieId(movie.getId());
            boolean isCurrentlyOnHero = !existingHeroBanners.isEmpty();

            // Calculate popularity heuristic score
            double statusBonus = movie.getStatus() == MovieStatus.NOW_SHOWING ? 40.0 : 25.0;
            double showtimeBonus = Math.min(showtimeCount * 5.0, 35.0);
            double recencyBonus = 0.0;
            if (movie.getReleaseDate() != null) {
                long daysSinceRelease = java.time.temporal.ChronoUnit.DAYS.between(movie.getReleaseDate(), LocalDate.now());
                if (daysSinceRelease >= 0 && daysSinceRelease <= 30) {
                    recencyBonus = (30 - daysSinceRelease) * 0.5;
                }
            }

            double hotScore = statusBonus + showtimeBonus + recencyBonus;

            long estTickets = (long) (showtimeCount * 42L);
            BigDecimal estRevenue = BigDecimal.valueOf(estTickets * 95000L);
            double estOccupancy = showtimeCount > 0 ? 0.72 : 0.0;
            double avgRating = 4.8;
            long reviewCount = estTickets / 10;
            long wishlistCount = estTickets / 5;

            HotMovieSuggestionResponse response = new HotMovieSuggestionResponse(
                    movie.getId(),
                    movie.getTitle(),
                    movie.getPosterUrl(),
                    movie.getAvatarUrl() != null ? movie.getAvatarUrl() : movie.getPosterUrl(),
                    movie.getTrailerUrl(),
                    movie.getDurationMinutes(),
                    movie.getAgeRating() != null ? movie.getAgeRating().name() : "P",
                    movie.getStatus().name(),
                    estTickets,
                    showtimeCount,
                    estRevenue,
                    BigDecimal.valueOf(estOccupancy).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                    avgRating,
                    reviewCount,
                    wishlistCount,
                    BigDecimal.valueOf(hotScore).setScale(1, RoundingMode.HALF_UP).doubleValue(),
                    isCurrentlyOnHero
            );

            evaluatedList.add(new HotMovieCandidate(response, hotScore, isCurrentlyOnHero));
        }

        // Sort descending by score, prioritizing movies not yet on hero
        evaluatedList.sort(Comparator
                .comparing(HotMovieCandidate::isCurrentlyOnHero)
                .thenComparing(HotMovieCandidate::score, Comparator.reverseOrder())
        );

        return evaluatedList.stream()
                .limit(maxResults)
                .map(HotMovieCandidate::response)
                .toList();
    }

    private record HotMovieCandidate(HotMovieSuggestionResponse response, double score, boolean isCurrentlyOnHero) {}
}
