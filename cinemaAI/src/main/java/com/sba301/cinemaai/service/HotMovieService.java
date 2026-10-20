package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.response.banner.HotMovieSuggestionResponse;
import java.util.List;

public interface HotMovieService {

    List<HotMovieSuggestionResponse> getHotMovieSuggestions(int limit);
}
