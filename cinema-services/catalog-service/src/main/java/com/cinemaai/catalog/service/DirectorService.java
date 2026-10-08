package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.movie.DirectorRequest;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.movie.DirectorResponse;
import com.cinemaai.catalog.entity.Director;

public interface DirectorService {

    PageResponse<DirectorResponse> searchDirectors(String keyword, int page, int size);

    DirectorResponse getDirector(Long id);

    DirectorResponse create(DirectorRequest request);

    DirectorResponse update(Long id, DirectorRequest request);

    void delete(Long id);

    Director findById(Long id);
}
