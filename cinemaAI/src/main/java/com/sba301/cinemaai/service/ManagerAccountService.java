package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.user.AdminManagerCreateRequest;
import com.sba301.cinemaai.dto.response.user.UserProfileResponse;
import java.util.List;

public interface ManagerAccountService {
    UserProfileResponse create(AdminManagerCreateRequest request);
    List<Long> assignCinemas(Long userId, List<Long> cinemaIds);
    List<Long> assignedCinemaIds(Long userId);
    void requireCinemaAccess(Long userId, Long cinemaId);
}
