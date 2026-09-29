package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.CinemaAudiencePrice;
import com.cinemaai.catalog.enums.AudienceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CinemaAudiencePriceRepository extends JpaRepository<CinemaAudiencePrice, Long> {

    List<CinemaAudiencePrice> findByCinemaId(Long cinemaId);

    Optional<CinemaAudiencePrice> findByCinemaIdAndAudienceType(Long cinemaId, AudienceType audienceType);

    boolean existsByCinemaId(Long cinemaId);
}
