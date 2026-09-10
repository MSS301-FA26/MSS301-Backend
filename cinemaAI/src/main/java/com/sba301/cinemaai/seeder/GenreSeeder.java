package com.sba301.cinemaai.seeder;

import com.sba301.cinemaai.entity.Genre;
import com.sba301.cinemaai.repository.GenreRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(20)
@RequiredArgsConstructor
public class GenreSeeder implements Seeder {

    private final GenreRepository genreRepository;

    @Override
    @Transactional
    public void seed() {
        Map<String, String> genres = Map.ofEntries(
                Map.entry("Action", "Action movies"),
                Map.entry("Drama", "Drama movies"),
                Map.entry("Comedy", "Comedy movies"),
                Map.entry("Horror", "Horror movies"),
                Map.entry("Romance", "Romance movies"),
                Map.entry("Sci-Fi", "Science fiction movies"),
                Map.entry("Thriller", "Thriller movies"),
                Map.entry("Adventure", "Adventure movies"),
                Map.entry("Animation", "Animated movies"),
                Map.entry("Family", "Family friendly movies"),
                Map.entry("Crime", "Crime movies"),
                Map.entry("Musical", "Musical movies")
        );

        genres.forEach((name, description) -> {
            if (!genreRepository.existsByName(name)) {
                genreRepository.save(new Genre(name, description));
            }
        });
    }
}
