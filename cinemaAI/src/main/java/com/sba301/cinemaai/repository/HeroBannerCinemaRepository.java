package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.HeroBannerCinema;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeroBannerCinemaRepository extends JpaRepository<HeroBannerCinema, Long> {

    List<HeroBannerCinema> findByHeroBannerId(Long heroBannerId);

    List<HeroBannerCinema> findByCinemaId(Long cinemaId);

    void deleteByHeroBannerId(Long heroBannerId);
}
