package com.sba301.cinemaai.ai.client;

import com.sba301.cinemaai.ai.dto.request.ChatRequest;
import com.sba301.cinemaai.ai.dto.response.AIChatResult;
import com.sba301.cinemaai.ai.dto.response.RecommendMovieResponse;
import com.sba301.cinemaai.ai.dto.response.RecommendStatsResponse;
import com.sba301.cinemaai.config.AIProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIClient {

    private final RestTemplate restTemplate;
    private final AIProperties aiProperties;

    private String url(String path) {
        return aiProperties.getUrl() + path;
    }

    public List<RecommendMovieResponse> content(Long movieId) {
        try {
            RecommendMovieResponse[] result = restTemplate.getForObject(
                    url("/recommend/content/" + movieId),
                    RecommendMovieResponse[].class
            );
            return result == null ? List.of() : Arrays.asList(result);
        } catch (Exception e) {
            log.warn("AI Service unavailable for content recommendation (movieId={}): {}", movieId, e.getMessage());
            return List.of();
        }
    }

    public List<RecommendMovieResponse> collaborative(Long userId) {
        try {
            RecommendMovieResponse[] result = restTemplate.getForObject(
                    url("/recommend/collaborative/" + userId),
                    RecommendMovieResponse[].class
            );
            return result == null ? List.of() : Arrays.asList(result);
        } catch (Exception e) {
            log.warn("AI Service unavailable for collaborative recommendation (userId={}): {}", userId, e.getMessage());
            return List.of();
        }
    }

    public RecommendStatsResponse stats() {
        try {
            return restTemplate.getForObject(url("/recommend/stats"), RecommendStatsResponse.class);
        } catch (Exception e) {
            log.warn("AI Service unavailable for recommend stats: {}", e.getMessage());
            return null;
        }
    }

    public AIChatResult chat(ChatRequest request) {
        try {
            AIChatResult result = restTemplate.postForObject(url("/chat"), request, AIChatResult.class);
            return result != null ? result : fallback(request);
        } catch (RestClientException e) {
            log.warn("AI Service unavailable for chat: {}", e.getMessage());
            return fallback(request);
        }
    }

    private AIChatResult fallback(ChatRequest request) {
        AIChatResult result = new AIChatResult();
        result.setMessage("Xin lỗi, tôi đang gặp sự cố. Vui lòng thử lại sau.");
        result.setConversationId(request.getConversationId());
        return result;
    }
}
