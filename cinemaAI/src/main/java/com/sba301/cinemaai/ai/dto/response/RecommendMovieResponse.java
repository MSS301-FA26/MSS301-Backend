package com.sba301.cinemaai.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommendMovieResponse {


    private Long movieId;


    private String title;


    private String posterUrl;


    private Double similarity;

    // Explanation metadata từ AiService: collaborative | content | fallback_rating | fallback_popularity
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
