package com.cinemaai.identity.service;

import com.cinemaai.identity.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserCinemaAssignmentService {

    void assignCinema(User user, Long cinemaId, User assignedBy);

    Optional<Long> getCinemaIdByUserId(Long userId);

    Map<Long, Long> getCinemaIdsByUserIds(Collection<Long> userIds);

    List<Long> getUserIdsByCinemaId(Long cinemaId);

    void removeAssignment(Long userId);
}
