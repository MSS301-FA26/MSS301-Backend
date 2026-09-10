package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByUserId(Long userId);

}
