package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.response.recommendation.RecommendMovieResponse;
import com.cinemaai.catalog.dto.response.recommendation.RecommendStatsResponse;
import java.util.List;

public interface RecommendationService {

    List<RecommendMovieResponse> content(Long movieId);

    List<RecommendMovieResponse> collaborative(Long userId);

    RecommendStatsResponse stats();
}
