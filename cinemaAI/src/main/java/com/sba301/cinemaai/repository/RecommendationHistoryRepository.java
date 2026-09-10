package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.RecommendationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationHistoryRepository extends JpaRepository<RecommendationHistory, Long> {
}
