package com.sba301.cinemaai.ai.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class ContentRecommendRequest {

    private Long movieId;


    private String description;


    private String director;


    private String actors;


    private List<String> genres;


}
