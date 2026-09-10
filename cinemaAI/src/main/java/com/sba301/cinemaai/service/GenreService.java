package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.movie.GenreRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.movie.GenreResponse;
import com.sba301.cinemaai.entity.Genre;
import java.util.List;

public interface GenreService {

    List<GenreResponse> getGenres();

    PageResponse<GenreResponse> getGenres(int page, int size);

    GenreResponse getGenre(Long id);

    GenreResponse create(GenreRequest request);

    GenreResponse update(Long id, GenreRequest request);

    void delete(Long id);

    Genre findById(Long id);
}
