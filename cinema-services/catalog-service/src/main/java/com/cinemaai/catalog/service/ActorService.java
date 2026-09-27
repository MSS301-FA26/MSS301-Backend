package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.movie.ActorRequest;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.movie.ActorResponse;
import com.cinemaai.catalog.entity.Actor;
import java.util.List;

public interface ActorService {

    PageResponse<ActorResponse> searchAdminActors(String keyword, int page, int size);

    PageResponse<ActorResponse> searchPublicActors(String keyword, int page, int size);

    ActorResponse getPublicActor(Long id);

    ActorResponse create(ActorRequest request);

    ActorResponse update(Long id, ActorRequest request);

    void delete(Long id);

    Actor findById(Long id);
}
