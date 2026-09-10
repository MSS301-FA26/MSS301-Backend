package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.LoyaltyPoint;
import com.sba301.cinemaai.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoyaltyPointRepository extends JpaRepository<LoyaltyPoint, Long> {

    Optional<LoyaltyPoint> findByUser(User user);

    boolean existsByUserId(Long userId);
}
