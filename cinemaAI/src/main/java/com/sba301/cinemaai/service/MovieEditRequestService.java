package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.movie.MovieEditProposalRequest;
import com.sba301.cinemaai.dto.response.movie.MovieEditRequestResponse;
import java.util.List;

public interface MovieEditRequestService {
    MovieEditRequestResponse submit(Long movieId, Long requesterId, MovieEditProposalRequest proposal);
    List<MovieEditRequestResponse> myRequests(Long userId);
    List<MovieEditRequestResponse> pendingRequests();
    List<MovieEditRequestResponse> allRequests();
    MovieEditRequestResponse approve(Long requestId, Long reviewerId);
    MovieEditRequestResponse reject(Long requestId, Long reviewerId, String reason);
}
