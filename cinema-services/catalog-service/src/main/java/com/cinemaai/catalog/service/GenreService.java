package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.movie.GenreRequest;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.movie.GenreResponse;
import com.cinemaai.catalog.entity.Genre;
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
