package com.cinemaai.identity.service.impl;

import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.entity.UserCinemaAssignment;
import com.cinemaai.identity.enums.AuditActionType;
import com.cinemaai.identity.repository.UserCinemaAssignmentRepository;
import com.cinemaai.identity.service.AuditLogService;
import com.cinemaai.identity.service.UserCinemaAssignmentService;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCinemaAssignmentServiceImpl implements UserCinemaAssignmentService {

    private final UserCinemaAssignmentRepository assignmentRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public void assignCinema(User user, Long cinemaId, User assignedBy) {
        if (user == null || cinemaId == null) {
            throw new IllegalArgumentException("User and cinemaId must not be null");
        }

        Optional<UserCinemaAssignment> existingOpt = assignmentRepository.findByUserId(user.getId());
        Long oldCinemaId = null;

        if (existingOpt.isPresent()) {
            UserCinemaAssignment assignment = existingOpt.get();
            oldCinemaId = assignment.getCinemaId();
            assignment.setCinemaId(cinemaId);
            assignment.setAssignedBy(assignedBy);
            assignmentRepository.save(assignment);
        } else {
            UserCinemaAssignment newAssignment = new UserCinemaAssignment(user, cinemaId, assignedBy);
            assignmentRepository.save(newAssignment);
        }

        String detail = String.format("Assigned user %s (ID: %d) to cinema ID %d (previous cinema ID: %s) by %s",
                user.getEmail(), user.getId(), cinemaId, oldCinemaId != null ? oldCinemaId : "NONE",
                assignedBy != null ? assignedBy.getEmail() : "SYSTEM");

        auditLogService.record(AuditActionType.UPDATE, "USER_CINEMA_ASSIGNMENT", user.getId(), detail);
        log.info(detail);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> getCinemaIdByUserId(Long userId) {
        if (userId == null) return Optional.empty();
        return assignmentRepository.findByUserId(userId).map(UserCinemaAssignment::getCinemaId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getCinemaIdsByUserIds(Collection<Long> userIds) {
        Map<Long, Long> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) return result;
        for (Long uid : userIds) {
            assignmentRepository.findByUserId(uid)
                    .ifPresent(assignment -> result.put(uid, assignment.getCinemaId()));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getUserIdsByCinemaId(Long cinemaId) {
        if (cinemaId == null) return List.of();
        return assignmentRepository.findByCinemaId(cinemaId).stream()
                .map(a -> a.getUser().getId())
                .toList();
    }

    @Override
    @Transactional
    public void removeAssignment(Long userId) {
        if (userId == null) return;
        assignmentRepository.findByUserId(userId).ifPresent(assignment -> {
            assignmentRepository.delete(assignment);
            auditLogService.record(AuditActionType.DELETE, "USER_CINEMA_ASSIGNMENT", userId,
                    "Removed cinema assignment for user ID: " + userId);
        });
    }
}
