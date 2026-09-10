package com.sba301.cinemaai.ai.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CollaborativeRecommendRequest {


    private Long userId;


    private List<Long> watchedMovies;


    private List<Long> ratedMovies;


}
