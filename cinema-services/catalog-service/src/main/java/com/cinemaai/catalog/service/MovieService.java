package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.movie.MovieCreateRequest;
import com.cinemaai.catalog.dto.request.movie.MovieStatusUpdateRequest;
import com.cinemaai.catalog.dto.request.movie.MovieUpdateRequest;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.movie.MovieResponse;
import com.cinemaai.catalog.enums.MovieStatus;
import java.time.LocalDate;
import java.util.List;

public interface MovieService {

    PageResponse<MovieResponse> searchPublic(String keyword, MovieStatus status, Long genreId,
                                             LocalDate fromDate, LocalDate toDate, int page, int size);

    PageResponse<MovieResponse> searchAdmin(String keyword, MovieStatus status, Long genreId,
                                            LocalDate fromDate, LocalDate toDate, int page, int size);

    MovieResponse getPublic(Long id);

    MovieResponse getAdmin(Long id);

    MovieResponse create(MovieCreateRequest request);

    MovieResponse update(Long id, MovieUpdateRequest request);

    List<MovieResponse> getMoviesByActor(Long actorId);

    MovieResponse updateStatus(Long id, MovieStatusUpdateRequest request);

    void delete(Long id);
}
