package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.response.banner.HotMovieSuggestionResponse;
import java.util.List;

public interface HotMovieService {

    List<HotMovieSuggestionResponse> getHotMovieSuggestions(int limit);
}
