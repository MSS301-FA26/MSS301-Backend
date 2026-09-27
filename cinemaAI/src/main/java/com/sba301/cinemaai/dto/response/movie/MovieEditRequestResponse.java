package com.sba301.cinemaai.dto.response.movie;

import com.sba301.cinemaai.enums.MovieEditRequestStatus;
import java.time.LocalDateTime;

public record MovieEditRequestResponse(Long id, Long movieId, String movieTitle, Long requestedBy,
        String requesterName, MovieEditRequestStatus status, String reason, String rejectionReason,
        String proposedData, Long reviewedBy, LocalDateTime createdAt, LocalDateTime reviewedAt) {

    public MovieEditRequestResponse(Long id, Long movieId, Long requestedBy,
            MovieEditRequestStatus status, String reason, String rejectionReason,
            Long reviewedBy, LocalDateTime createdAt, LocalDateTime reviewedAt) {
        this(id, movieId, null, requestedBy, null, status, reason, rejectionReason, null, reviewedBy, createdAt, reviewedAt);
    }
}

