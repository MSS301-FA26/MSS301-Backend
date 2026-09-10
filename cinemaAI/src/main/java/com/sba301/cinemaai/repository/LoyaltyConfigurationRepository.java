package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.LoyaltyConfiguration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyConfigurationRepository extends JpaRepository<LoyaltyConfiguration, Long> {
    Optional<LoyaltyConfiguration> findTopByOrderByIdAsc();
}
