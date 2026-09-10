package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.enums.CinemaStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CinemaRepository extends JpaRepository<Cinema, Long> {

    Optional<Cinema> findByName(String name);

    Optional<Cinema> findFirstByOrderByIdAsc();

    Optional<Cinema> findFirstByStatus(CinemaStatus status);

    List<Cinema> findByCity(String city);
}
