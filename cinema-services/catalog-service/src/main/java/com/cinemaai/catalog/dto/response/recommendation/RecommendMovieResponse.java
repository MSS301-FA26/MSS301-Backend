package com.cinemaai.catalog.dto.response.recommendation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommendMovieResponse {

    private Long movieId;
    private String title;
    private String posterUrl;
    private Double similarity;
    private String source;
    private String reason;
    private Double predictedRating;
    private Integer neighborCount;
    private String anchorTitle;
    private List<String> matchedGenres;
    private List<String> matchedActors;
    private Boolean sameDirector;
    private String directorName;
    private Double avgRating;
    private Integer ratingCount;
    private Integer bookingCount;
}
