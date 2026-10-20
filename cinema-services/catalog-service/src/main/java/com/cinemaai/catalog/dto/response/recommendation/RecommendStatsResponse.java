package com.cinemaai.catalog.dto.response.recommendation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendStatsResponse {
    private Integer reviewCount;
    private Integer reviewerCount;
    private Integer paidBookingCount;
    private Integer embeddedMovieCount;
}
