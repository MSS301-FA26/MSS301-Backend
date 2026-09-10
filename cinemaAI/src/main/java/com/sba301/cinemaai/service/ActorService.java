package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.movie.ActorRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.movie.ActorResponse;
import com.sba301.cinemaai.entity.Actor;
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
