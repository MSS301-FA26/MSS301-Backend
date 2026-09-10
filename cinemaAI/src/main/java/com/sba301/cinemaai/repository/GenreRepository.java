package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Genre;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    Optional<Genre> findByName(String name);

    boolean existsByName(String name);
}
