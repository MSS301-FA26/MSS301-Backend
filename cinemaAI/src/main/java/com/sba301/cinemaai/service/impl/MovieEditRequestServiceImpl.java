package com.sba301.cinemaai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba301.cinemaai.dto.request.movie.MovieEditProposalRequest;
import com.sba301.cinemaai.dto.request.movie.MovieUpdateRequest;
import com.sba301.cinemaai.dto.response.movie.MovieEditRequestResponse;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.MovieEditRequest;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.MovieApprovalStatus;
import com.sba301.cinemaai.enums.MovieEditRequestStatus;
import com.sba301.cinemaai.enums.MoviePublicationStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.ConflictException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.MovieEditRequestRepository;
import com.sba301.cinemaai.repository.MovieRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.MovieEditRequestService;
import com.sba301.cinemaai.service.MovieService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MovieEditRequestServiceImpl implements MovieEditRequestService {
    private final MovieEditRequestRepository requestRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final MovieService movieService;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public MovieEditRequestResponse submit(Long movieId, Long requesterId, MovieEditProposalRequest proposal) {
        Movie movie = movieRepository.findForEditRequest(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found"));
        if (movie.getApprovalStatus() != MovieApprovalStatus.APPROVED
                || movie.getPublicationStatus() != MoviePublicationStatus.PUBLISHED) {
            throw new BadRequestException("Only published movies require an edit request");
        }
        if (requestRepository.existsByMovieIdAndStatus(movieId, MovieEditRequestStatus.PENDING)) {
            throw new ConflictException("A movie edit request is already pending");
        }
        MovieUpdateRequest proposed = proposal.proposed();
        if (proposed == null || proposed.durationMinutes() == null || proposed.durationMinutes() <= 0
                || proposed.actorIds() == null || proposed.actorIds().isEmpty()
                || proposed.mainActorIds() == null || proposed.mainActorIds().isEmpty()) {
            throw new BadRequestException("Complete movie details are required");
        }
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new NotFoundException("Manager not found"));
        try {
            MovieEditRequest request = requestRepository.save(new MovieEditRequest(movie, requester,
                    objectMapper.writeValueAsString(proposed), proposal.reason().trim()));
            auditLogService.record(AuditActionType.CREATE, "MOVIE_EDIT_REQUEST", request.getId(),
                    "Movie " + movieId + " edit requested by user " + requesterId);
            return response(request);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Invalid movie proposal");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieEditRequestResponse> myRequests(Long userId) {
        return requestRepository.findByRequestedByIdOrderByCreatedAtDesc(userId).stream().map(this::response).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieEditRequestResponse> pendingRequests() {
        return requestRepository.findByStatusOrderByCreatedAtAsc(MovieEditRequestStatus.PENDING)
                .stream().map(this::response).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovieEditRequestResponse> allRequests() {
        return requestRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::response).toList();
    }

    @Override
    @Transactional
    public MovieEditRequestResponse approve(Long requestId, Long reviewerId) {
        MovieEditRequest request = pendingForReview(requestId);
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("Admin not found"));
        if (reviewerId.equals(request.getRequestedBy().getId())) {
            throw new BadRequestException("Cannot approve your own movie edit request");
        }
        try {
            MovieUpdateRequest proposed = objectMapper.readValue(request.getProposedData(), MovieUpdateRequest.class);
            movieService.update(request.getMovie().getId(), proposed, reviewerId);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Stored movie proposal is invalid");
        }
        request.approve(reviewer);
        auditLogService.record(AuditActionType.UPDATE, "MOVIE_EDIT_REQUEST", requestId,
                "Approved movie " + request.getMovie().getId() + " edit by user " + reviewerId);
        return response(request);
    }

    @Override
    @Transactional
    public MovieEditRequestResponse reject(Long requestId, Long reviewerId, String reason) {
        if (reason == null || reason.isBlank()) throw new BadRequestException("Rejection reason is required");
        MovieEditRequest request = pendingForReview(requestId);
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("Admin not found"));
        request.reject(reviewer, reason.trim());
        auditLogService.record(AuditActionType.UPDATE, "MOVIE_EDIT_REQUEST", requestId,
                "Rejected movie " + request.getMovie().getId() + " edit by user " + reviewerId);
        return response(request);
    }

    private MovieEditRequest pendingForReview(Long id) {
        MovieEditRequest request = requestRepository.findForReview(id)
                .orElseThrow(() -> new NotFoundException("Movie edit request not found"));
        if (request.getStatus() != MovieEditRequestStatus.PENDING) {
            throw new ConflictException("REQUEST_ALREADY_REVIEWED");
        }
        return request;
    }

    private MovieEditRequestResponse response(MovieEditRequest request) {
        String movieTitle = request.getMovie() != null ? request.getMovie().getTitle() : null;
        String requesterName = request.getRequestedBy() != null ? request.getRequestedBy().getFullName() : null;
        return new MovieEditRequestResponse(request.getId(), request.getMovie().getId(),
                movieTitle, request.getRequestedBy().getId(), requesterName, request.getStatus(),
                request.getReason(), request.getRejectionReason(), request.getProposedData(),
                request.getReviewedBy() == null ? null : request.getReviewedBy().getId(),
                request.getCreatedAt(), request.getReviewedAt());
    }
}

