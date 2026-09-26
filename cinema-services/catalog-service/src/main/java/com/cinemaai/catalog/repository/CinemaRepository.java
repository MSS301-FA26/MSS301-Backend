package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.Cinema;
import com.cinemaai.catalog.enums.CinemaStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CinemaRepository extends JpaRepository<Cinema, Long> {

    Optional<Cinema> findByName(String name);
    Optional<Cinema> findFirstByName(String name);

    Optional<Cinema> findFirstByOrderByIdAsc();

    Optional<Cinema> findFirstByStatus(CinemaStatus status);

    List<Cinema> findByCity(String city);
    List<Cinema> findAllByOrderByIdAsc();
    List<Cinema> findByStatusOrderByIdAsc(CinemaStatus status);
}
