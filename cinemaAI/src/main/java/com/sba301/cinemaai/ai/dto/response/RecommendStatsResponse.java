package com.sba301.cinemaai.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Số liệu thật hệ gợi ý đang phân tích — dùng cho modal "Cách hoạt động" trên FE.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendStatsResponse {
    private Integer reviewCount;
    private Integer reviewerCount;
    private Integer paidBookingCount;
    private Integer embeddedMovieCount;
}
