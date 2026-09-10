package com.sba301.cinemaai.ai.service.impl;

import com.sba301.cinemaai.ai.client.AIClient;
import com.sba301.cinemaai.ai.dto.response.RecommendMovieResponse;
import com.sba301.cinemaai.ai.dto.response.RecommendStatsResponse;
import com.sba301.cinemaai.ai.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final AIClient client;

    @Override
    public List<RecommendMovieResponse> content(Long movieId) {
        return client.content(movieId);
    }

    @Override
    public List<RecommendMovieResponse> collaborative(Long userId) {
        return client.collaborative(userId);
    }

    @Override
    public RecommendStatsResponse stats() {
        return client.stats();
    }

}
