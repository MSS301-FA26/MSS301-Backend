package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.user.AdminManagerCreateRequest;
import com.sba301.cinemaai.dto.response.user.UserProfileResponse;
import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.entity.ManagerCinemaAssignment;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.RoleName;
import com.sba301.cinemaai.enums.UserStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.ConflictException;
import com.sba301.cinemaai.exception.ForbiddenException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.CinemaRepository;
import com.sba301.cinemaai.repository.ManagerCinemaAssignmentRepository;
import com.sba301.cinemaai.repository.UserProfileRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.repository.UserRoleRepository;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.ManagerAccountService;
import com.sba301.cinemaai.service.UserRoleService;
import com.sba301.cinemaai.service.UserService;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManagerAccountServiceImpl implements ManagerAccountService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final CinemaRepository cinemaRepository;
    private final ManagerCinemaAssignmentRepository assignmentRepository;
    private final UserRoleService userRoleService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public UserProfileResponse create(AdminManagerCreateRequest request) {
        List<Cinema> cinemas = resolveCinemas(request.cinemaIds());
        if (userRepository.existsByEmail(request.email())) throw new ConflictException("Email already exists");
        if (request.phone() != null && !request.phone().isBlank() && userProfileRepository.existsByPhone(request.phone())) {
            throw new ConflictException("Phone already exists");
        }
        User manager = new User(request.email(), passwordEncoder.encode(request.password()), request.fullName(), request.phone());
        manager.setEmailVerified(true);
        manager.setStatus(UserStatus.ACTIVE);
        userRepository.save(manager);
        userRoleService.assignRole(manager, RoleName.MANAGER);
        cinemas.forEach(cinema -> assignmentRepository.save(new ManagerCinemaAssignment(manager, cinema)));
        auditLogService.record(AuditActionType.CREATE, "USER", manager.getId(), "MANAGER " + manager.getEmail());
        return userService.toProfile(manager);
    }

    @Override
    @Transactional
    public List<Long> assignCinemas(Long userId, List<Long> cinemaIds) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Manager not found"));
        boolean manager = userRoleRepository.findByUserId(userId).stream()
                .anyMatch(role -> role.getRole().getName() == RoleName.MANAGER);
        if (!manager) throw new BadRequestException("User is not a manager");
        List<Cinema> cinemas = resolveCinemas(cinemaIds);
        assignmentRepository.deleteByUserId(userId);
        assignmentRepository.flush();
        cinemas.forEach(cinema -> assignmentRepository.save(new ManagerCinemaAssignment(user, cinema)));
        auditLogService.record(AuditActionType.UPDATE, "USER", userId, "Manager cinema assignment updated");
        return cinemas.stream().map(Cinema::getId).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> assignedCinemaIds(Long userId) {
        return assignmentRepository.findByUserId(userId).stream()
                .map(assignment -> assignment.getCinema().getId()).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void requireCinemaAccess(Long userId, Long cinemaId) {
        if (cinemaId == null || !assignmentRepository.existsByUserIdAndCinemaId(userId, cinemaId)) {
            throw new ForbiddenException("CINEMA_ACCESS_DENIED");
        }
    }

    private List<Cinema> resolveCinemas(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BadRequestException("CINEMA_SCOPE_REQUIRED");
        }
        Set<Long> uniqueIds = new HashSet<>(ids);
        List<Cinema> cinemas = cinemaRepository.findAllById(uniqueIds);
        if (cinemas.size() != uniqueIds.size()) throw new NotFoundException("Cinema not found");
        return cinemas;
    }
}
