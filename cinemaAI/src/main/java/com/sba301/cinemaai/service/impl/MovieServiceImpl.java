package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.movie.MovieCreateRequest;
import com.sba301.cinemaai.dto.response.movie.ActorResponse;
import com.sba301.cinemaai.dto.response.movie.MovieResponse;
import com.sba301.cinemaai.dto.request.movie.MovieStatusUpdateRequest;
import com.sba301.cinemaai.dto.request.movie.MovieUpdateRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.entity.Actor;
import com.sba301.cinemaai.entity.Genre;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.MovieActor;
import com.sba301.cinemaai.entity.MovieGenre;
import com.sba301.cinemaai.enums.AgeRating;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.MovieStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.ConflictException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.mapper.MovieMapper;
import com.sba301.cinemaai.repository.MovieGenreRepository;
import com.sba301.cinemaai.repository.MovieRepository;
import com.sba301.cinemaai.repository.ActorRepository;
import com.sba301.cinemaai.repository.MovieActorRepository;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.GenreService;
import com.sba301.cinemaai.service.MovieService;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final MovieActorRepository movieActorRepository;
    private final ActorRepository actorRepository;
    private final GenreService genreService;
    private final MovieMapper movieMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "publicMovies",
            key = "{#keyword, #status, #genreId, #fromDate, #toDate, #page, #size}"
    )
    public PageResponse<MovieResponse> searchPublic(
            String keyword,
            MovieStatus status,
            Long genreId,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        MovieStatus effectiveStatus = status == null ? null : status;
        Specification<Movie> spec = buildSpec(keyword, effectiveStatus, genreId, fromDate, toDate, true);
        return mapPage(movieRepository.findAll(spec, pageable(page, size)));
    }

    @Transactional(readOnly = true)
    public PageResponse<MovieResponse> searchAdmin(
            String keyword,
            MovieStatus status,
            Long genreId,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        return mapPage(movieRepository.findAll(buildSpec(keyword, status, genreId, fromDate, toDate, false), pageable(page, size)));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "publicMovieDetails", key = "#id")
    public MovieResponse getPublic(Long id) {
        Movie movie = findById(id);
        if (movie.getStatus() == MovieStatus.INACTIVE) {
            throw new NotFoundException("Movie not found");
        }
        return toResponse(movie);
    }

    @Transactional(readOnly = true)
    public MovieResponse getAdmin(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse create(MovieCreateRequest request) {
        if (movieRepository.existsByTitle(request.title())) {
            throw new ConflictException("Movie title already exists");
        }

        validateReleaseWindow(request.releaseDate(), request.endDate());
        MovieStatus computedStatus = resolveStatusFromDates(request.releaseDate(), request.endDate());
        Movie movie = new Movie(request.title(), request.durationMinutes(), computedStatus);
        List<Actor> actors = resolveActors(request.actorIds());
        Set<Long> mainActorIds = validateMainActorIds(actors, request.mainActorIds());
        String actorNames = actorNamesText(actors);
        String mainActorNames = actorNamesText(actors.stream()
                .filter(actor -> mainActorIds.contains(actor.getId()))
                .toList());
        applyMovieFields(movie, request.description(), request.releaseDate(), request.endDate(), request.trailerUrl(), request.posterUrl(),
                request.avatarUrl(), request.language(), request.subtitleLanguage(), request.ageRating(),
                request.director(), mainActorNames, actorNames, computedStatus);
        Movie saved = movieRepository.save(movie);
        replaceGenres(saved, request.genreIds());
        replaceActors(saved, actors, mainActorIds);
        auditLogService.record(AuditActionType.CREATE, "MOVIE", saved.getId(), saved.getTitle());
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse update(Long id, MovieUpdateRequest request) {
        Movie movie = findById(id);
        if (movie.getStatus() != MovieStatus.UPCOMING) {
            throw new BadRequestException("Only UPCOMING movies can be updated");
        }
        movieRepository.findByTitle(request.title())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Movie title already exists");
                });
        validateReleaseWindow(request.releaseDate(), request.endDate());
        MovieStatus computedStatus = resolveStatusFromDates(request.releaseDate(), request.endDate());
        List<Actor> actors = resolveActors(request.actorIds());
        Set<Long> mainActorIds = validateMainActorIds(actors, request.mainActorIds());
        String actorNames = actorNamesText(actors);
        String mainActorNames = actorNamesText(actors.stream()
                .filter(actor -> mainActorIds.contains(actor.getId()))
                .toList());
        applyMovieFields(movie, request.description(), request.releaseDate(), request.endDate(), request.trailerUrl(), request.posterUrl(),
                request.avatarUrl(), request.language(), request.subtitleLanguage(), request.ageRating(),
                request.director(), mainActorNames, actorNames, computedStatus);
        movie.setTitle(request.title());
        movie.setDurationMinutes(request.durationMinutes());
        replaceGenres(movie, request.genreIds());
        replaceActors(movie, actors, mainActorIds);
        auditLogService.record(AuditActionType.UPDATE, "MOVIE", movie.getId(), movie.getTitle());
        return toResponse(movie);
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> getMoviesByActor(Long actorId) {
        Actor actor = findActorById(actorId);
        return movieActorRepository.findByActor(actor)
                .stream()
                .map(MovieActor::getMovie)
                .filter(movie -> movie.getStatus() != MovieStatus.INACTIVE)
                .sorted((left, right) -> {
                    LocalDate leftDate = left.getReleaseDate();
                    LocalDate rightDate = right.getReleaseDate();
                    if (leftDate == null && rightDate == null) {
                        return right.getId().compareTo(left.getId());
                    }
                    if (leftDate == null) {
                        return 1;
                    }
                    if (rightDate == null) {
                        return -1;
                    }
                    int dateCompare = rightDate.compareTo(leftDate);
                    return dateCompare != 0 ? dateCompare : right.getId().compareTo(left.getId());
                })
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse updateStatus(Long id, MovieStatusUpdateRequest request) {
        Movie movie = findById(id);
        movie.setStatus(request.status());
        auditLogService.record(AuditActionType.UPDATE, "MOVIE", movie.getId(),
                movie.getTitle() + " -> " + request.status());
        return toResponse(movie);
    }

    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public void delete(Long id) {
        Movie movie = findById(id);
        movie.setStatus(MovieStatus.INACTIVE);
        auditLogService.record(AuditActionType.DELETE, "MOVIE", movie.getId(), movie.getTitle());
    }

    private void applyMovieFields(
            Movie movie,
            String description,
            LocalDate releaseDate,
            LocalDate endDate,
            String trailerUrl,
            String posterUrl,
            String avatarUrl,
            String language,
            String subtitleLanguage,
            String ageRating,
            String director,
            String mainActors,
            String castList,
            MovieStatus status
    ) {
        movie.setDescription(description);
        movie.setReleaseDate(releaseDate);
        movie.setEndDate(endDate);
        movie.setTrailerUrl(trailerUrl);
        movie.setPosterUrl(posterUrl);
        movie.setAvatarUrl(avatarUrl);
        movie.setLanguage(language);
        movie.setSubtitleLanguage(subtitleLanguage);
        movie.setAgeRating(AgeRating.from(ageRating));
        movie.setDirector(director);
        movie.setMainActors(mainActors);
        movie.setCastList(castList);
        movie.setStatus(status);
    }

    private void validateReleaseWindow(LocalDate releaseDate, LocalDate endDate) {
        if (releaseDate == null) {
            throw new BadRequestException("Release date is required");
        }
        if (endDate == null) {
            throw new BadRequestException("End date is required");
        }
        if (releaseDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Release date must be today or in the future");
        }
        if (endDate.isBefore(releaseDate)) {
            throw new BadRequestException("End date must be on or after release date");
        }
    }

    private MovieStatus resolveStatusFromDates(LocalDate releaseDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(releaseDate)) {
            return MovieStatus.UPCOMING;
        }
        if (today.isAfter(endDate)) {
            return MovieStatus.ENDED;
        }
        return MovieStatus.NOW_SHOWING;
    }

    private void replaceActors(Movie movie, List<Actor> actors, Set<Long> mainActorIds) {
        movieActorRepository.deleteByMovie(movie);
        movieActorRepository.flush();
        actors.stream()
                .distinct()
                .map(actor -> new MovieActor(movie, actor, mainActorIds.contains(actor.getId())))
                .forEach(movieActorRepository::save);
    }

    private List<Actor> resolveActors(List<Long> actorIds) {
        return actorIds.stream()
                .distinct()
                .map(this::findActorById)
                .toList();
    }

    private Set<Long> validateMainActorIds(List<Actor> actors, List<Long> requestedMainActorIds) {
        Set<Long> actorIds = actors.stream().map(Actor::getId).collect(Collectors.toSet());
        Set<Long> mainActorIds = new HashSet<>(requestedMainActorIds);
        if (!actorIds.containsAll(mainActorIds)) {
            throw new BadRequestException("Main actor ids must be included in actor ids");
        }
        return mainActorIds;
    }

    private String actorNamesText(List<Actor> actors) {
        return String.join(", ", actors.stream().map(Actor::getName).toList());
    }

    private Actor findActorById(Long id) {
        return actorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Actor not found"));
    }

    private void replaceGenres(Movie movie, List<Long> genreIds) {
        movieGenreRepository.deleteByMovie(movie);
        movieGenreRepository.flush();
        if (genreIds == null) {
            return;
        }
        genreIds.stream()
                .distinct()
                .map(genreService::findById)
                .map(genre -> new MovieGenre(movie, genre))
                .forEach(movieGenreRepository::save);
    }

    private PageResponse<MovieResponse> mapPage(Page<Movie> page) {
        return new PageResponse<>(
                mapMovies(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    private MovieResponse toResponse(Movie movie) {
        return mapMovies(List.of(movie)).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Movie not found"));
    }

    private List<MovieResponse> mapMovies(List<Movie> movies) {
        if (movies.isEmpty()) {
            return List.of();
        }

        List<Long> movieIds = movies.stream().map(Movie::getId).toList();

        Map<Long, List<Genre>> genresByMovieId = movieGenreRepository.findWithGenreByMovieIdIn(movieIds)
                .stream()
                .collect(Collectors.groupingBy(
                        movieGenre -> movieGenre.getMovie().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(MovieGenre::getGenre, Collectors.toList())
                ));

        Map<Long, List<MovieActor>> actorLinksByMovieId = movieActorRepository.findWithActorByMovieIdIn(movieIds)
                .stream()
                .collect(Collectors.groupingBy(
                        movieActor -> movieActor.getMovie().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<Long> actorIds = actorLinksByMovieId.values()
                .stream()
                .flatMap(List::stream)
                .map(movieActor -> movieActor.getActor().getId())
                .distinct()
                .toList();
        Map<Long, Long> movieCountsByActorId = actorIds.isEmpty()
                ? Map.of()
                : actorRepository.findWithMovieCountByIdIn(actorIds)
                        .stream()
                        .collect(Collectors.toMap(
                                result -> result.getActor().getId(),
                                result -> result.getMovieCount(),
                                (left, right) -> left
                        ));

        List<MovieResponse> responses = new ArrayList<>(movies.size());
        for (Movie movie : movies) {
            List<MovieActor> movieActorLinks = actorLinksByMovieId.getOrDefault(movie.getId(), List.of());
            List<ActorResponse> actors = movieActorLinks.stream()
                    .map(MovieActor::getActor)
                    .map(actor -> movieMapper.toActorResponse(actor, movieCountsByActorId.getOrDefault(actor.getId(), 0L)))
                    .toList();
            List<Long> mainActorIds = movieActorLinks.stream()
                    .filter(MovieActor::isMainActor)
                    .map(movieActor -> movieActor.getActor().getId())
                    .toList();

            responses.add(movieMapper.toMovieResponse(
                    movie,
                    genresByMovieId.getOrDefault(movie.getId(), List.of()),
                    actors,
                    mainActorIds
            ));
        }
        return responses;
    }

    private Movie findById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Movie not found"));
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "releaseDate").and(Sort.by("id")));
    }

    private Specification<Movie> buildSpec(
            String keyword,
            MovieStatus status,
            Long genreId,
            LocalDate fromDate,
            LocalDate toDate,
            boolean publicOnly
    ) {
        return (root, query, builder) -> {
            query.distinct(true);
            var predicate = builder.conjunction();
            if (publicOnly) {
                predicate = builder.and(predicate, builder.notEqual(root.get("status"), MovieStatus.INACTIVE));
            }
            if (status != null) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("title")), pattern),
                        builder.like(builder.lower(root.get("director")), pattern),
                        builder.like(builder.lower(root.get("language")), pattern)
                ));
            }
            if (fromDate != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("releaseDate"), fromDate));
            }
            if (toDate != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("releaseDate"), toDate));
            }
            if (genreId != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<MovieGenre> movieGenreRoot = subquery.from(MovieGenre.class);
                subquery.select(movieGenreRoot.get("movie").get("id"))
                        .where(builder.equal(movieGenreRoot.get("genre").get("id"), genreId));
                predicate = builder.and(predicate, root.get("id").in(subquery));
            }
            return predicate;
        };
    }
}
