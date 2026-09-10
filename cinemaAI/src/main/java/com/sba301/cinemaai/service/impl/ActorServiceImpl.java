package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.movie.ActorRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.movie.ActorResponse;
import com.sba301.cinemaai.entity.Actor;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.MovieActor;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.exception.ConflictException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.mapper.MovieMapper;
import com.sba301.cinemaai.repository.ActorRepository;
import com.sba301.cinemaai.repository.MovieActorRepository;
import com.sba301.cinemaai.service.ActorService;
import com.sba301.cinemaai.service.AuditLogService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ActorServiceImpl implements ActorService {

    private final ActorRepository actorRepository;
    private final MovieActorRepository movieActorRepository;
    private final MovieMapper movieMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PageResponse<ActorResponse> searchAdminActors(String keyword, int page, int size) {
        Pageable pageRequest = pageable(page, size);
        var results = StringUtils.hasText(keyword)
                ? actorRepository.searchAdminWithMovieCount(keyword.trim(), pageRequest)
                : actorRepository.findAdminWithMovieCount(pageRequest);
        return PageResponse.from(results.map(result -> movieMapper.toActorResponse(result.getActor(), result.getMovieCount())));
    }

    @Transactional(readOnly = true)
    public PageResponse<ActorResponse> searchPublicActors(String keyword, int page, int size) {
        Pageable pageRequest = pageable(page, size);
        var results = StringUtils.hasText(keyword)
                ? actorRepository.searchPublicWithMovieCount(keyword.trim(), pageRequest)
                : actorRepository.findPublicWithMovieCount(pageRequest);
        return PageResponse.from(results.map(result -> movieMapper.toActorResponse(result.getActor(), result.getMovieCount())));
    }

    @Transactional(readOnly = true)
    public ActorResponse getPublicActor(Long id) {
        return actorRepository.findPublicWithMovieCountById(id)
                .map(result -> movieMapper.toActorResponse(result.getActor(), result.getMovieCount()))
                .orElseThrow(() -> new NotFoundException("Actor not found"));
    }

    @Transactional
    public ActorResponse create(ActorRequest request) {
        String name = request.name().trim();
        if (actorRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Actor name already exists");
        }
        Actor saved = actorRepository.save(new Actor(name, request.biography(), request.avatarUrl()));
        auditLogService.record(AuditActionType.CREATE, "ACTOR", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public ActorResponse update(Long id, ActorRequest request) {
        Actor actor = findById(id);
        String name = request.name().trim();
        actorRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Actor name already exists");
        });
        actor.setName(name);
        actor.setBiography(request.biography());
        actor.setAvatarUrl(request.avatarUrl());
        refreshMovieActorMetadata(actor);
        auditLogService.record(AuditActionType.UPDATE, "ACTOR", actor.getId(), actor.getName());
        return toResponse(actor);
    }

    @Transactional
    public void delete(Long id) {
        Actor actor = findById(id);
        if (movieActorRepository.countByActor(actor) > 0) {
            throw new ConflictException("Actor is used by movies");
        }
        actorRepository.delete(actor);
        auditLogService.record(AuditActionType.DELETE, "ACTOR", actor.getId(), actor.getName());
    }

    public Actor findById(Long id) {
        return actorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Actor not found"));
    }

    private ActorResponse toResponse(Actor actor) {
        return actorRepository.findWithMovieCountByIdIn(List.of(actor.getId()))
                .stream()
                .findFirst()
                .map(result -> movieMapper.toActorResponse(result.getActor(), result.getMovieCount()))
                .orElseThrow(() -> new NotFoundException("Actor not found"));
    }

    private void refreshMovieActorMetadata(Actor actor) {
        movieActorRepository.findByActor(actor).stream()
                .map(MovieActor::getMovie)
                .distinct()
                .forEach(this::refreshActorMetadata);
    }

    private void refreshActorMetadata(Movie movie) {
        List<MovieActor> actorLinks = movieActorRepository.findByMovie(movie);
        String castList = joinActorNames(actorLinks);
        String mainActors = joinActorNames(actorLinks.stream()
                .filter(MovieActor::isMainActor)
                .toList());
        movie.setMainActors(mainActors);
        movie.setCastList(castList);
    }

    private String joinActorNames(List<MovieActor> actorLinks) {
        return actorLinks.stream()
                .map(link -> link.getActor().getName())
                .collect(Collectors.joining(", "));
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize);
    }
}
