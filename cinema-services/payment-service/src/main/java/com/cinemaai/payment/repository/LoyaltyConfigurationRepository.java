package com.cinemaai.payment.repository;

import com.cinemaai.payment.entity.LoyaltyConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoyaltyConfigurationRepository extends JpaRepository<LoyaltyConfiguration, Long> {
    Optional<LoyaltyConfiguration> findByCinemaId(Long cinemaId);
    Optional<LoyaltyConfiguration> findByCinemaIdIsNull();
}
