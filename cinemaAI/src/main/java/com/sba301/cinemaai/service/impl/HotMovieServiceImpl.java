package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.response.banner.HotMovieSuggestionResponse;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.HeroBanner;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.enums.BannerStatus;
import com.sba301.cinemaai.enums.BookingStatus;
import com.sba301.cinemaai.enums.MoviePublicationStatus;
import com.sba301.cinemaai.enums.MovieStatus;
import com.sba301.cinemaai.repository.BookingRepository;
import com.sba301.cinemaai.repository.HeroBannerRepository;
import com.sba301.cinemaai.repository.MovieRepository;
import com.sba301.cinemaai.repository.ReviewRepository;
import com.sba301.cinemaai.repository.ShowtimeRepository;
import com.sba301.cinemaai.repository.WishlistRepository;
import com.sba301.cinemaai.service.HotMovieService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotMovieServiceImpl implements HotMovieService {

    private final MovieRepository movieRepository;
    private final BookingRepository bookingRepository;
    private final com.sba301.cinemaai.repository.BookingSeatRepository bookingSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistRepository wishlistRepository;
    private final HeroBannerRepository heroBannerRepository;

    private static final Set<BookingStatus> PAID_STATUSES = Set.of(
            BookingStatus.PAID,
            BookingStatus.USED
    );

    @Override
    @Transactional(readOnly = true)
    public List<HotMovieSuggestionResponse> getHotMovieSuggestions(int limit) {
        int maxResults = limit > 0 ? Math.min(limit, 20) : 6;

        // Fetch candidate movies: NOT INACTIVE, NOT ENDED, and PUBLISHED
        List<Movie> candidateMovies = movieRepository.findAll().stream()
                .filter(m -> m.getStatus() != MovieStatus.INACTIVE && m.getStatus() != MovieStatus.ENDED)
                .filter(m -> m.getPublicationStatus() == MoviePublicationStatus.PUBLISHED)
                .toList();

        if (candidateMovies.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        List<HeroBanner> activeBanners = heroBannerRepository.findActivePublicBanners(BannerStatus.PUBLISHED, now);
        Set<Long> bannerMovieIds = activeBanners.stream()
                .filter(b -> b.getLinkedMovie() != null)
                .map(b -> b.getLinkedMovie().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<HotMovieSuggestionResponse> suggestions = new ArrayList<>();

        for (Movie movie : candidateMovies) {
            Long movieId = movie.getId();

            // Calculate bookings for this movie
            List<Booking> bookings = bookingRepository.findAll().stream()
                    .filter(b -> b.getShowtime() != null && b.getShowtime().getMovie() != null)
                    .filter(b -> b.getShowtime().getMovie().getId().equals(movieId))
                    .filter(b -> PAID_STATUSES.contains(b.getStatus()))
                    .toList();

            long bookingCount = bookings.size();
            BigDecimal revenue = bookings.stream()
                    .map(Booking::getTotalAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Estimate / calculate actual tickets sold
            long ticketsSold = 0;
            if (!bookings.isEmpty()) {
                try {
                    ticketsSold = bookingSeatRepository.findByBookingIn(bookings).size();
                } catch (Exception e) {
                    ticketsSold = bookingCount * 2;
                }
            }

            // Reviews & rating
            double avgRating = 8.5; // reasonable fallback
            long reviewCount = 0;
            try {
                var reviews = reviewRepository.findByMovieAndStatus(movie, com.sba301.cinemaai.enums.ReviewStatus.VISIBLE);
                if (reviews != null && !reviews.isEmpty()) {
                    reviewCount = reviews.size();
                    avgRating = reviews.stream().mapToInt(com.sba301.cinemaai.entity.Review::getRating).average().orElse(8.5);
                }
            } catch (Exception e) {
                log.debug("Could not calculate reviews for movie {}: {}", movieId, e.getMessage());
            }

            // Wishlist count
            long wishlistCount = 0;
            try {
                wishlistCount = wishlistRepository.countByMovie(movie);
            } catch (Exception e) {
                log.debug("Could not count wishlists for movie {}: {}", movieId, e.getMessage());
            }

            // Occupancy rate calculation (0% - 100%)
            double occupancyRate = 0.0;
            try {
                var showtimes = showtimeRepository.findByMovie(movie);
                if (showtimes != null && !showtimes.isEmpty()) {
                    long totalCapacity = showtimes.stream()
                            .mapToLong(st -> st.getRoom() != null && st.getRoom().getRowCount() > 0 && st.getRoom().getColumnCount() > 0
                                    ? ((long) st.getRoom().getRowCount() * st.getRoom().getColumnCount()) : 80L)
                            .sum();
                    if (totalCapacity > 0) {
                        occupancyRate = Math.min(100.0, ((double) ticketsSold / totalCapacity) * 100.0);
                    }
                }
            } catch (Exception e) {
                log.debug("Could not calculate occupancy for movie {}: {}", movieId, e.getMessage());
            }

            // Centralized Rule-Based Hot Score Formula:
            // Score = (ticketsSold * 2.0) + (bookingCount * 5.0) + (occupancyRate * 1.5) + (avgRating * 12.0) + (wishlistCount * 3.0)
            double hotScore = (ticketsSold * 2.0)
                    + (bookingCount * 5.0)
                    + (occupancyRate * 1.5)
                    + (avgRating * 12.0)
                    + (wishlistCount * 3.0);

            // Slight boost if NOW_SHOWING
            if (movie.getStatus() == MovieStatus.NOW_SHOWING) {
                hotScore += 25.0;
            }

            BigDecimal roundedRevenue = revenue.setScale(0, RoundingMode.HALF_UP);
            double roundedOccupancy = BigDecimal.valueOf(occupancyRate).setScale(1, RoundingMode.HALF_UP).doubleValue();
            double roundedRating = BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP).doubleValue();
            double roundedScore = BigDecimal.valueOf(hotScore).setScale(1, RoundingMode.HALF_UP).doubleValue();

            suggestions.add(new HotMovieSuggestionResponse(
                    movieId,
                    movie.getTitle(),
                    movie.getPosterUrl(),
                    movie.getAvatarUrl() != null ? movie.getAvatarUrl() : movie.getPosterUrl(),
                    movie.getTrailerUrl(),
                    movie.getDurationMinutes(),
                    movie.getAgeRating() != null ? movie.getAgeRating().name() : "P",
                    movie.getStatus().name(),
                    ticketsSold,
                    bookingCount,
                    roundedRevenue,
                    roundedOccupancy,
                    roundedRating,
                    reviewCount,
                    wishlistCount,
                    roundedScore,
                    bannerMovieIds.contains(movieId)
            ));
        }

        // Sort by hotScore descending
        suggestions.sort(Comparator.comparingDouble(HotMovieSuggestionResponse::hotScore).reversed());

        return suggestions.stream().limit(maxResults).toList();
    }
}
