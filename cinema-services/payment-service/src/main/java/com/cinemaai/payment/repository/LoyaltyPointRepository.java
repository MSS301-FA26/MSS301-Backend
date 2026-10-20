package com.cinemaai.payment.repository;

import com.cinemaai.payment.entity.LoyaltyPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoyaltyPointRepository extends JpaRepository<LoyaltyPoint, Long> {
    Optional<LoyaltyPoint> findByUserId(Long userId);
    Optional<LoyaltyPoint> findByUserEmail(String userEmail);
}
