package com.sba301.cinemaai.ai.service;

import com.sba301.cinemaai.ai.dto.response.RecommendMovieResponse;
import com.sba301.cinemaai.ai.dto.response.RecommendStatsResponse;

import java.util.List;

public interface RecommendationService {

    List<RecommendMovieResponse> content(Long movieId);

    List<RecommendMovieResponse> collaborative(Long userId);

    RecommendStatsResponse stats();

}
