package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.movie.MovieCreateRequest;
import com.sba301.cinemaai.dto.request.movie.MovieRejectRequest;
import com.sba301.cinemaai.dto.request.movie.MovieStatusUpdateRequest;
import com.sba301.cinemaai.dto.request.movie.MovieUpdateRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.movie.ActorResponse;
import com.sba301.cinemaai.dto.response.movie.MovieApprovalHistoryResponse;
import com.sba301.cinemaai.dto.response.movie.MovieResponse;
import com.sba301.cinemaai.entity.Actor;
import com.sba301.cinemaai.entity.Genre;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.MovieActor;
import com.sba301.cinemaai.entity.MovieApprovalHistory;
import com.sba301.cinemaai.entity.MovieGenre;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.AgeRating;
import com.sba301.cinemaai.enums.ApprovalAction;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.MovieApprovalStatus;
import com.sba301.cinemaai.enums.MoviePublicationStatus;
import com.sba301.cinemaai.enums.MovieStatus;
import com.sba301.cinemaai.enums.RoleName;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.ConflictException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.mapper.MovieMapper;
import com.sba301.cinemaai.repository.ActorRepository;
import com.sba301.cinemaai.repository.MovieActorRepository;
import com.sba301.cinemaai.repository.MovieApprovalHistoryRepository;
import com.sba301.cinemaai.repository.MovieGenreRepository;
import com.sba301.cinemaai.repository.MovieRepository;
import com.sba301.cinemaai.repository.ShowtimeRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.repository.UserRoleRepository;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.GenreService;
import com.sba301.cinemaai.service.MovieService;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final MovieApprovalHistoryRepository movieApprovalHistoryRepository;
    private final UserRepository userRepository;
    private final ShowtimeRepository showtimeRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
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
        Specification<Movie> spec = buildSpec(keyword, status, MovieApprovalStatus.APPROVED, MoviePublicationStatus.PUBLISHED, genreId, fromDate, toDate, true);
        return mapPage(movieRepository.findAll(spec, pageable(page, size)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MovieResponse> searchAdmin(
            String keyword,
            MovieStatus status,
            MovieApprovalStatus approvalStatus,
            MoviePublicationStatus publicationStatus,
            Long genreId,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        Specification<Movie> spec = buildSpec(keyword, status, approvalStatus, publicationStatus, genreId, fromDate, toDate, false);
        return mapPage(movieRepository.findAll(spec, pageable(page, size)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "publicMovieDetails", key = "#id")
    public MovieResponse getPublic(Long id) {
        Movie movie = findById(id);
        if (movie.getStatus() == MovieStatus.INACTIVE
                || movie.getApprovalStatus() != MovieApprovalStatus.APPROVED
                || movie.getPublicationStatus() != MoviePublicationStatus.PUBLISHED) {
            throw new NotFoundException("Movie not found");
        }
        return toResponse(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponse getAdmin(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse create(MovieCreateRequest request) {
        return create(request, null);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse create(MovieCreateRequest request, Long userId) {
        if (movieRepository.existsByTitleIgnoreCase(request.title())) {
            throw new ConflictException("Tên phim đã tồn tại trong hệ thống");
        }

        if (request.releaseDate() != null && request.endDate() != null) {
            if (request.endDate().isBefore(request.releaseDate())) {
                throw new BadRequestException("Ngày kết thúc phải bằng hoặc sau ngày phát hành");
            }
        }

        MovieStatus computedStatus = resolveStatusFromDates(request.releaseDate(), request.endDate());
        Movie movie = new Movie(request.title(), request.durationMinutes(), computedStatus);
        movie.setApprovalStatus(MovieApprovalStatus.DRAFT);
        movie.setPublicationStatus(MoviePublicationStatus.UNPUBLISHED);

        User creator = userId != null ? userRepository.findById(userId).orElse(null) : resolveActor();
        movie.setSubmittedBy(creator);

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

        auditLogService.record(AuditActionType.CREATE, "MOVIE", saved.getId(), saved.getTitle() + " (Bản nháp - DRAFT)");
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse update(Long id, MovieUpdateRequest request) {
        return update(id, request, null);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse update(Long id, MovieUpdateRequest request, Long userId) {
        Movie movie = findById(id);

        movieRepository.findByTitleIgnoreCase(request.title())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Tên phim đã tồn tại trong hệ thống");
                });

        if (request.releaseDate() != null && request.endDate() != null) {
            if (request.endDate().isBefore(request.releaseDate())) {
                throw new BadRequestException("Ngày kết thúc phải bằng hoặc sau ngày phát hành");
            }
        }

        // When modifying an existing movie, allow in-place updates regardless of current status
        if (movie.getPublicationStatus() == MoviePublicationStatus.PUBLISHED) {
            movie.setPublicationStatus(MoviePublicationStatus.UNPUBLISHED);
        }
        if (movie.getApprovalStatus() == MovieApprovalStatus.APPROVED) {
            movie.setApprovalStatus(MovieApprovalStatus.DRAFT);
            movie.setApprovedAt(null);
            movie.setApprovedBy(null);
        }

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

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse submitForApproval(Long id, Long userId) {
        Movie movie = findById(id);
        if (movie.getPublicationStatus() == MoviePublicationStatus.ARCHIVED) {
            throw new BadRequestException("MOVIE_ALREADY_ARCHIVED: Không thể gửi duyệt phim đã lưu trữ!");
        }
        if (movie.getApprovalStatus() == MovieApprovalStatus.PENDING_APPROVAL) {
            // Already pending approval: allow refreshing submission
            movie.setSubmittedAt(LocalDateTime.now());
            if (userId != null) {
                userRepository.findById(userId).ifPresent(movie::setSubmittedBy);
            }
            Movie saved = movieRepository.save(movie);
            auditLogService.record(AuditActionType.UPDATE, "MOVIE", saved.getId(), "Cập nhật yêu cầu chờ duyệt: " + saved.getTitle());
            return toResponse(saved);
        }

        // Section X: Backend validation of required fields
        if (!StringUtils.hasText(movie.getTitle())) {
            throw new BadRequestException("MOVIE_TITLE_REQUIRED: Tên phim không được để trống khi gửi duyệt!");
        }
        if (movie.getDurationMinutes() <= 0) {
            throw new BadRequestException("MOVIE_DURATION_REQUIRED: Thời lượng phim phải lớn hơn 0 phút!");
        }
        if (!StringUtils.hasText(movie.getPosterUrl())) {
            throw new BadRequestException("MOVIE_POSTER_REQUIRED: Áp phích (Poster) là bắt buộc khi gửi duyệt!");
        }
        if (!StringUtils.hasText(movie.getDescription())) {
            throw new BadRequestException("MOVIE_DESCRIPTION_REQUIRED: Mô tả/Nội dung phim là bắt buộc khi gửi duyệt!");
        }
        if (movie.getReleaseDate() == null) {
            throw new BadRequestException("MOVIE_RELEASE_DATE_REQUIRED: Ngày khởi chiếu là bắt buộc khi gửi duyệt!");
        }
        if (movie.getEndDate() == null) {
            throw new BadRequestException("MOVIE_END_DATE_REQUIRED: Ngày kết thúc là bắt buộc khi gửi duyệt!");
        }
        if (movie.getEndDate().isBefore(movie.getReleaseDate())) {
            throw new BadRequestException("Ngày kết thúc phải bằng hoặc sau ngày phát hành");
        }
        if (movie.getAgeRating() == null) {
            throw new BadRequestException("MOVIE_AGE_RATING_REQUIRED: Phân loại độ tuổi là bắt buộc khi gửi duyệt!");
        }
        List<MovieGenre> genres = movieGenreRepository.findByMovie(movie);
        if (genres.isEmpty()) {
            throw new BadRequestException("MOVIE_GENRE_REQUIRED: Phim phải có ít nhất 1 thể loại khi gửi duyệt!");
        }

        MovieApprovalStatus fromStatus = movie.getApprovalStatus();
        ApprovalAction action = (fromStatus == MovieApprovalStatus.REJECTED || fromStatus == MovieApprovalStatus.APPROVED) 
                ? ApprovalAction.RESUBMITTED 
                : ApprovalAction.SUBMITTED;

        User submitter = userId != null ? userRepository.findById(userId).orElse(null) : resolveActor();
        movie.setApprovalStatus(MovieApprovalStatus.PENDING_APPROVAL);
        movie.setPublicationStatus(MoviePublicationStatus.UNPUBLISHED);
        movie.setSubmittedAt(LocalDateTime.now());
        movie.setSubmittedBy(submitter);
        movie.setApprovedAt(null);
        movie.setApprovedBy(null);
        movie.setRejectionReason(null);
        movie.setRejectedAt(null);
        movie.setRejectedBy(null);

        Movie saved = movieRepository.save(movie);

        MovieApprovalHistory history = new MovieApprovalHistory(
                saved,
                submitter,
                action,
                fromStatus,
                MovieApprovalStatus.PENDING_APPROVAL,
                action == ApprovalAction.RESUBMITTED ? "Gửi lại sau khi chỉnh sửa phản hồi từ chối hoặc cập nhật bản cũ" : "Gửi duyệt phim"
        );
        movieApprovalHistoryRepository.save(history);
        auditLogService.record(AuditActionType.UPDATE, "MOVIE", saved.getId(), "Submitted for approval: " + saved.getTitle());
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse withdrawApproval(Long id, Long userId) {
        Movie movie = findById(id);
        if (movie.getApprovalStatus() != MovieApprovalStatus.PENDING_APPROVAL) {
            throw new BadRequestException("MOVIE_NOT_PENDING_APPROVAL: Chỉ phim đang CHỜ DUYỆT mới có thể rút lại yêu cầu!");
        }

        User actor = userId != null ? userRepository.findById(userId).orElse(null) : resolveActor();
        MovieApprovalStatus fromStatus = movie.getApprovalStatus();
        movie.setApprovalStatus(MovieApprovalStatus.DRAFT);
        Movie saved = movieRepository.save(movie);

        MovieApprovalHistory history = new MovieApprovalHistory(
                saved,
                actor,
                ApprovalAction.WITHDRAWN,
                fromStatus,
                MovieApprovalStatus.DRAFT,
                "Rút lại yêu cầu duyệt"
        );
        movieApprovalHistoryRepository.save(history);
        auditLogService.record(AuditActionType.UPDATE, "MOVIE", saved.getId(), "Withdrawn from approval: " + saved.getTitle());
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse approveMovie(Long id, Long approverId, String note) {
        Movie movie = findById(id);
        if (movie.getApprovalStatus() != MovieApprovalStatus.PENDING_APPROVAL) {
            throw new BadRequestException("MOVIE_NOT_PENDING_APPROVAL: Chỉ phim đang CHỜ DUYỆT mới có thể phê duyệt!");
        }

        User approver = approverId != null ? userRepository.findById(approverId).orElse(null) : resolveActor();
        boolean isApproverAdmin = isUserAdmin(approver);
        // Section XL: Separation of duty - non-admin submitter cannot approve own movie
        if (!isApproverAdmin && movie.getSubmittedBy() != null && approver != null && movie.getSubmittedBy().getId().equals(approver.getId())) {
            throw new BadRequestException("UNAUTHORIZED_MOVIE_APPROVAL: Người gửi duyệt không được phép tự duyệt phim của chính mình!");
        }

        MovieApprovalStatus fromStatus = movie.getApprovalStatus();
        movie.setApprovalStatus(MovieApprovalStatus.APPROVED);
        movie.setApprovedAt(LocalDateTime.now());
        movie.setApprovedBy(approver);
        movie.setRejectionReason(null);
        Movie saved = movieRepository.save(movie);

        MovieApprovalHistory history = new MovieApprovalHistory(
                saved,
                approver,
                ApprovalAction.APPROVED,
                fromStatus,
                MovieApprovalStatus.APPROVED,
                note != null && !note.isBlank() ? note.trim() : "Đã phê duyệt phim thành công"
        );
        movieApprovalHistoryRepository.save(history);
        auditLogService.record(AuditActionType.UPDATE, "MOVIE", saved.getId(), "Approved movie: " + saved.getTitle());
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse rejectMovie(Long id, Long rejecterId, MovieRejectRequest request) {
        Movie movie = findById(id);
        if (movie.getApprovalStatus() != MovieApprovalStatus.PENDING_APPROVAL) {
            throw new BadRequestException("MOVIE_NOT_PENDING_APPROVAL: Chỉ phim đang CHỜ DUYỆT mới có thể từ chối!");
        }
        if (request == null || !StringUtils.hasText(request.reason())) {
            throw new BadRequestException("MOVIE_REJECTION_REASON_REQUIRED: Bắt buộc phải nhập lý do từ chối!");
        }

        User rejecter = rejecterId != null ? userRepository.findById(rejecterId).orElse(null) : resolveActor();
        MovieApprovalStatus fromStatus = movie.getApprovalStatus();
        movie.setApprovalStatus(MovieApprovalStatus.REJECTED);
        movie.setRejectedAt(LocalDateTime.now());
        movie.setRejectedBy(rejecter);
        movie.setRejectionReason(request.reason().trim());
        Movie saved = movieRepository.save(movie);

        MovieApprovalHistory history = new MovieApprovalHistory(
                saved,
                rejecter,
                ApprovalAction.REJECTED,
                fromStatus,
                MovieApprovalStatus.REJECTED,
                request.reason().trim()
        );
        movieApprovalHistoryRepository.save(history);
        auditLogService.record(AuditActionType.UPDATE, "MOVIE", saved.getId(), "Rejected movie: " + saved.getTitle() + " - Reason: " + request.reason().trim());
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse publishMovie(Long id, Long userId) {
        Movie movie = findById(id);
        if (movie.getApprovalStatus() != MovieApprovalStatus.APPROVED) {
            throw new BadRequestException("MOVIE_NOT_APPROVED: Chỉ những bộ phim đã được DUYỆT NỘI BỘ (APPROVED) mới có thể xuất bản ra website!");
        }
        if (movie.getPublicationStatus() == MoviePublicationStatus.PUBLISHED) {
            return toResponse(movie);
        }

        User actor = userId != null ? userRepository.findById(userId).orElse(null) : resolveActor();
        movie.setPublicationStatus(MoviePublicationStatus.PUBLISHED);
        movie.setPublishedAt(LocalDateTime.now());
        Movie saved = movieRepository.save(movie);

        MovieApprovalHistory history = new MovieApprovalHistory(
                saved,
                actor,
                ApprovalAction.PUBLISHED,
                movie.getApprovalStatus(),
                movie.getApprovalStatus(),
                "Xuất bản phim lên website công khai"
        );
        movieApprovalHistoryRepository.save(history);
        auditLogService.record(AuditActionType.UPDATE, "MOVIE", saved.getId(), "Published movie: " + saved.getTitle());
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse unpublishMovie(Long id, Long userId) {
        Movie movie = findById(id);
        if (movie.getPublicationStatus() != MoviePublicationStatus.PUBLISHED) {
            throw new BadRequestException("Chỉ phim đang CÔNG KHAI (PUBLISHED) mới có thể gỡ xuất bản!");
        }

        movie.setPublicationStatus(MoviePublicationStatus.UNPUBLISHED);
        Movie saved = movieRepository.save(movie);
        auditLogService.record(AuditActionType.UPDATE, "MOVIE", saved.getId(), "Unpublished movie: " + saved.getTitle());
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse archiveMovie(Long id, Long userId) {
        Movie movie = findById(id);
        if (movie.getPublicationStatus() == MoviePublicationStatus.ARCHIVED) {
            throw new BadRequestException("MOVIE_ALREADY_ARCHIVED: Phim đã ở trạng thái lưu trữ!");
        }

        User actor = userId != null ? userRepository.findById(userId).orElse(null) : resolveActor();
        movie.setPublicationStatus(MoviePublicationStatus.ARCHIVED);
        movie.setStatus(MovieStatus.INACTIVE);
        Movie saved = movieRepository.save(movie);

        MovieApprovalHistory history = new MovieApprovalHistory(
                saved,
                actor,
                ApprovalAction.ARCHIVED,
                movie.getApprovalStatus(),
                movie.getApprovalStatus(),
                "Lưu trữ phim (Ngừng chiếu và ẩn khỏi website)"
        );
        movieApprovalHistoryRepository.save(history);
        auditLogService.record(AuditActionType.UPDATE, "MOVIE", saved.getId(), "Archived movie: " + saved.getTitle());
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse unarchiveMovie(Long id, Long userId) {
        Movie movie = findById(id);
        if (movie.getPublicationStatus() != MoviePublicationStatus.ARCHIVED) {
            throw new BadRequestException("Chỉ phim đang ở trạng thái LƯU TRỮ mới có thể lấy ra lại!");
        }

        User actor = userId != null ? userRepository.findById(userId).orElse(null) : resolveActor();
        movie.setPublicationStatus(MoviePublicationStatus.UNPUBLISHED);

        if (movie.getApprovalStatus() == MovieApprovalStatus.APPROVED) {
            LocalDate today = LocalDate.now();
            if (movie.getReleaseDate() != null && !movie.getReleaseDate().isAfter(today)) {
                if (movie.getEndDate() != null && movie.getEndDate().isBefore(today)) {
                    movie.setStatus(MovieStatus.ENDED);
                } else {
                    movie.setStatus(MovieStatus.NOW_SHOWING);
                }
            } else {
                movie.setStatus(MovieStatus.UPCOMING);
            }
        } else {
            movie.setStatus(MovieStatus.UPCOMING);
        }

        Movie saved = movieRepository.save(movie);

        MovieApprovalHistory history = new MovieApprovalHistory(
                saved,
                actor,
                ApprovalAction.UNARCHIVED,
                MoviePublicationStatus.ARCHIVED.name(),
                MoviePublicationStatus.UNPUBLISHED.name(),
                "Khôi phục phim từ lưu trữ (Đưa về trạng thái chưa xuất bản)"
        );
        movieApprovalHistoryRepository.save(history);
        auditLogService.record(AuditActionType.UPDATE, "MOVIE", saved.getId(), "Unarchived movie: " + saved.getTitle());
        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public void delete(Long id) {
        Movie movie = findById(id);
        if (movie.getPublicationStatus() != MoviePublicationStatus.ARCHIVED) {
            throw new BadRequestException("Chỉ phim đã đưa vào LƯU TRỮ mới có thể xóa vĩnh viễn! Vui lòng chuyển phim sang Lưu trữ trước khi xóa.");
        }
        if (showtimeRepository.existsByMovie(movie)) {
            throw new ConflictException("MOVIE_HAS_SHOWTIMES: Không thể xóa vĩnh viễn phim đã có suất chiếu! Vui lòng giữ phim trong mục Lưu trữ để bảo toàn lịch sử đặt vé và dữ liệu hệ thống.");
        }
        movieGenreRepository.deleteByMovie(movie);
        movieActorRepository.deleteByMovie(movie);
        movieApprovalHistoryRepository.deleteByMovie(movie);
        movieRepository.delete(movie);
        auditLogService.record(AuditActionType.DELETE, "MOVIE", movie.getId(), movie.getTitle() + " (Đã xóa vĩnh viễn)");
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieApprovalHistoryResponse> getApprovalHistory(Long id) {
        Movie movie = findById(id);
        return movieApprovalHistoryRepository.findWithActorByMovieIdOrderByCreatedAtDesc(movie.getId())
                .stream()
                .map(h -> {
                    User actor = h.getActor();
                    String actorName = null;
                    if (actor != null && actor.getProfile() != null) {
                        actorName = actor.getProfile().getFullName();
                    }
                    return new MovieApprovalHistoryResponse(
                            h.getId(),
                            h.getMovie().getId(),
                            h.getAction(),
                            h.getFromStatus(),
                            h.getToStatus(),
                            h.getComment(),
                            actor != null ? actor.getId() : null,
                            actor != null ? actor.getEmail() : null,
                            actorName,
                            h.getCreatedAt()
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieResponse> getMoviesByActor(Long actorId) {
        Actor actor = findActorById(actorId);
        return movieActorRepository.findByActor(actor)
                .stream()
                .map(MovieActor::getMovie)
                .filter(movie -> movie.getStatus() != MovieStatus.INACTIVE
                        && movie.getApprovalStatus() == MovieApprovalStatus.APPROVED
                        && movie.getPublicationStatus() == MoviePublicationStatus.PUBLISHED)
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

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"publicMovies", "publicMovieDetails"}, allEntries = true)
    public MovieResponse updateStatus(Long id, MovieStatusUpdateRequest request) {
        Movie movie = findById(id);
        movie.setStatus(request.status());
        auditLogService.record(AuditActionType.UPDATE, "MOVIE", movie.getId(),
                movie.getTitle() + " -> " + request.status());
        return toResponse(movie);
    }

    private User resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            return null;
        }
        return userRepository.findByEmail(principal.email()).orElse(null);
    }

    private boolean isUserAdmin(User user) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            boolean hasAdminAuthority = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()) || "ADMIN".equalsIgnoreCase(a.getAuthority()));
            if (hasAdminAuthority) {
                return true;
            }
        }
        if (user != null && user.getId() != null) {
            return userRoleRepository.findByUserId(user.getId()).stream()
                    .anyMatch(ur -> ur.getRole() != null && RoleName.ADMIN == ur.getRole().getName());
        }
        return false;
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
        if (ageRating != null && !ageRating.isBlank()) {
            movie.setAgeRating(AgeRating.from(ageRating));
        }
        movie.setDirector(director);
        movie.setMainActors(mainActors);
        movie.setCastList(castList);
        movie.setStatus(status);
    }

    private MovieStatus resolveStatusFromDates(LocalDate releaseDate, LocalDate endDate) {
        if (releaseDate == null || endDate == null) {
            return MovieStatus.UPCOMING;
        }
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
        if (actorIds == null) {
            return List.of();
        }
        return actorIds.stream()
                .distinct()
                .map(this::findActorById)
                .toList();
    }

    private Set<Long> validateMainActorIds(List<Actor> actors, List<Long> requestedMainActorIds) {
        if (requestedMainActorIds == null || requestedMainActorIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> actorIds = actors.stream().map(Actor::getId).collect(Collectors.toSet());
        Set<Long> mainActorIds = new HashSet<>(requestedMainActorIds);
        if (!actorIds.containsAll(mainActorIds)) {
            throw new BadRequestException("Diễn viên chính phải nằm trong danh sách diễn viên");
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
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
    }

    private Specification<Movie> buildSpec(
            String keyword,
            MovieStatus status,
            MovieApprovalStatus approvalStatus,
            MoviePublicationStatus publicationStatus,
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
                predicate = builder.and(predicate, builder.equal(root.get("approvalStatus"), MovieApprovalStatus.APPROVED));
                predicate = builder.and(predicate, builder.equal(root.get("publicationStatus"), MoviePublicationStatus.PUBLISHED));
            } else {
                if (approvalStatus != null) {
                    predicate = builder.and(predicate, builder.equal(root.get("approvalStatus"), approvalStatus));
                }
                if (publicationStatus != null) {
                    predicate = builder.and(predicate, builder.equal(root.get("publicationStatus"), publicationStatus));
                }
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
