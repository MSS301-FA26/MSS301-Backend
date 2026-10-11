package com.cinemaai.identity.service.impl;

import com.cinemaai.identity.client.CatalogClient;
import com.cinemaai.identity.dto.request.user.AdminManagerCreateRequest;
import com.cinemaai.identity.dto.request.user.AdminStaffCreateRequest;
import com.cinemaai.identity.dto.request.user.AdminUserStatusUpdateRequest;
import com.cinemaai.identity.dto.request.user.ChangePasswordRequest;
import com.cinemaai.identity.dto.request.user.UserProfileUpdateRequest;
import com.cinemaai.identity.dto.response.user.UserAccessScopeResponse;
import com.cinemaai.identity.dto.response.user.UserProfileResponse;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.entity.UserProfile;
import com.cinemaai.identity.enums.AuditActionType;
import com.cinemaai.identity.enums.RoleName;
import com.cinemaai.identity.enums.UserStatus;
import com.cinemaai.identity.exception.BadRequestException;
import com.cinemaai.identity.exception.ConflictException;
import com.cinemaai.identity.exception.ForbiddenException;
import com.cinemaai.identity.exception.NotFoundException;
import com.cinemaai.identity.mapper.UserMapper;
import com.cinemaai.identity.repository.UserProfileRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.service.AuditLogService;
import com.cinemaai.identity.service.UserCinemaAssignmentService;
import com.cinemaai.identity.service.UserRoleService;
import com.cinemaai.identity.service.UserService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleService userRoleService;
    private final UserCinemaAssignmentService userCinemaAssignmentService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final CatalogClient catalogClient;

    @Override
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        User user = getByEmail(email);
        return toProfile(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String email, UserProfileUpdateRequest request) {
        User user = getByEmail(email);
        UserProfile profile = user.getProfile();
        profile.setFullName(request.fullName());
        if (!Objects.equals(profile.getPhone(), request.phone())) {
            profile.setPhoneVerified(false);
        }
        profile.setPhone(request.phone());
        user.setBirthYear(request.birthYear());
        return toProfile(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateAvatar(String email, String avatarUrl) {
        User user = getByEmail(email);
        user.getProfile().setAvatarUrl(avatarUrl);
        return toProfile(user);
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Confirm password does not match");
        }

        User user = getByEmail(email);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Old password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from old password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers(RoleName role) {
        List<User> users = (role != null)
                ? userRepository.findByRoleName(role)
                : userRepository.findAll();
        return users.stream()
                .map(this::toProfile)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsersForActor(RoleName role, String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            return getAllUsers(role);
        }

        User actor = getByEmail(actorEmail);
        List<String> actorRoles = userRoleService.getRoleNames(actor.getId());

        if (actorRoles.contains(RoleName.ADMIN.name())) {
            return getAllUsers(role);
        }

        if (actorRoles.contains(RoleName.MANAGER.name())) {
            Long managerCinemaId = userCinemaAssignmentService.getCinemaIdByUserId(actor.getId())
                    .orElseThrow(() -> new ForbiddenException("Quản lý chưa được phân công cụm rạp nào."));

            List<Long> cinemaUserIds = userCinemaAssignmentService.getUserIdsByCinemaId(managerCinemaId);
            return userRepository.findAllById(cinemaUserIds).stream()
                    .filter(u -> {
                        List<String> uRoles = userRoleService.getRoleNames(u.getId());
                        return uRoles.contains(RoleName.STAFF.name());
                    })
                    .map(this::toProfile)
                    .toList();
        }

        throw new ForbiddenException("Chỉ có ADMIN hoặc MANAGER mới có quyền xem danh sách nhân sự.");
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getById(Long id) {
        return toProfile(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getByIdForActor(Long id, String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            return getById(id);
        }

        User actor = getByEmail(actorEmail);
        List<String> actorRoles = userRoleService.getRoleNames(actor.getId());

        if (actorRoles.contains(RoleName.ADMIN.name())) {
            return getById(id);
        }

        if (actorRoles.contains(RoleName.MANAGER.name())) {
            User targetUser = findById(id);
            List<String> targetRoles = userRoleService.getRoleNames(targetUser.getId());
            // Manager can view Customer profiles (e.g. for loyalty audit trail, booking info)
            if (targetRoles.contains(RoleName.CUSTOMER.name())) {
                return toProfile(targetUser);
            }
            if (!targetRoles.contains(RoleName.STAFF.name())) {
                throw new ForbiddenException("Quản lý chỉ có quyền xem thông tin tài khoản Staff và Khách hàng.");
            }

            Long managerCinema = userCinemaAssignmentService.getCinemaIdByUserId(actor.getId())
                    .orElseThrow(() -> new ForbiddenException("Quản lý chưa được phân công cụm rạp nào."));
            Long staffCinema = userCinemaAssignmentService.getCinemaIdByUserId(targetUser.getId())
                    .orElse(null);

            if (!java.util.Objects.equals(managerCinema, staffCinema)) {
                throw new ForbiddenException("Quản lý chỉ được xem tài khoản Staff thuộc chính rạp của mình.");
            }

            return toProfile(targetUser);
        }

        throw new ForbiddenException("Không có quyền truy cập thông tin tài khoản.");
    }

    @Override
    @Transactional
    public UserProfileResponse createManager(AdminManagerCreateRequest request, String actorEmail) {
        if (request.cinemaId() == null) {
            throw new BadRequestException("Vui lòng chọn cụm rạp cho tài khoản Manager.");
        }
        catalogClient.validateActiveCinema(request.cinemaId());

        validateUniqueEmailAndPhone(request.email(), request.phone());

        User actor = actorEmail != null ? getByEmail(actorEmail) : null;
        User manager = userRepository.save(new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.phone(),
                request.birthYear()
        ));
        activateEmail(manager);
        userRoleService.assignRole(manager, RoleName.MANAGER);

        userCinemaAssignmentService.assignCinema(manager, request.cinemaId(), actor);
        auditLogService.record(AuditActionType.CREATE, "MANAGER", manager.getId(),
                "Created Manager for cinema ID " + request.cinemaId() + " by " + (actor != null ? actor.getEmail() : "SYSTEM"));

        return toProfile(manager);
    }

    @Override
    @Transactional
    public UserProfileResponse createStaff(AdminStaffCreateRequest request) {
        return createStaffForActor(request, null);
    }

    @Override
    @Transactional
    public UserProfileResponse createStaffForActor(AdminStaffCreateRequest request, String actorEmail) {
        Long targetCinemaId;
        User actor = actorEmail != null ? getByEmail(actorEmail) : null;

        if (actor != null) {
            List<String> actorRoles = userRoleService.getRoleNames(actor.getId());
            if (actorRoles.contains(RoleName.MANAGER.name())) {
                // IMPORTANT: Manager creates Staff -> Strictly use Manager's assigned cinema from DB!
                // Never trust branchId/cinemaId from client request!
                targetCinemaId = userCinemaAssignmentService.getCinemaIdByUserId(actor.getId())
                        .orElseThrow(() -> new ForbiddenException("Tài khoản Quản lý chưa được phân công cụm rạp nào."));
            } else if (actorRoles.contains(RoleName.ADMIN.name())) {
                targetCinemaId = request.cinemaId();
                if (targetCinemaId == null) {
                    throw new BadRequestException("Admin cần chỉ định cụm rạp khi tạo tài khoản nhân viên.");
                }
            } else {
                throw new ForbiddenException("Bạn không có quyền tạo tài khoản nhân viên.");
            }
        } else {
            targetCinemaId = request.cinemaId();
            if (targetCinemaId == null) {
                throw new BadRequestException("Cụm rạp là bắt buộc để tạo nhân viên.");
            }
        }

        validateUniqueEmailAndPhone(request.email(), request.phone());
        catalogClient.validateActiveCinema(targetCinemaId);

        User staff = userRepository.save(new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.phone(),
                request.birthYear()
        ));
        activateEmail(staff);
        userRoleService.assignRole(staff, RoleName.STAFF);

        userCinemaAssignmentService.assignCinema(staff, targetCinemaId, actor);
        auditLogService.record(AuditActionType.CREATE, "STAFF", staff.getId(),
                "Created Staff for cinema ID " + targetCinemaId + " by " + (actor != null ? actor.getEmail() : "SYSTEM"));

        return toProfile(staff);
    }

    @Override
    @Transactional
    public UserProfileResponse assignCinema(Long userId, Long cinemaId, String actorEmail) {
        if (cinemaId == null) {
            throw new BadRequestException("Cinema ID is required.");
        }
        catalogClient.validateActiveCinema(cinemaId);
        User user = findById(userId);
        User actor = actorEmail != null ? getByEmail(actorEmail) : null;

        userCinemaAssignmentService.assignCinema(user, cinemaId, actor);
        return toProfile(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateStatus(Long id, AdminUserStatusUpdateRequest request) {
        return updateStatusForActor(id, request, null);
    }

    @Override
    @Transactional
    public UserProfileResponse updateStatusForActor(Long id, AdminUserStatusUpdateRequest request, String actorEmail) {
        User targetUser = findById(id);
        User actor = actorEmail != null ? getByEmail(actorEmail) : null;

        if (actor != null) {
            List<String> actorRoles = userRoleService.getRoleNames(actor.getId());
            if (actorRoles.contains(RoleName.MANAGER.name()) && !actorRoles.contains(RoleName.ADMIN.name())) {
                List<String> targetRoles = userRoleService.getRoleNames(targetUser.getId());
                if (!targetRoles.contains(RoleName.STAFF.name())) {
                    throw new ForbiddenException("Quản lý chỉ có quyền khóa hoặc đổi trạng thái tài khoản Staff.");
                }

                Long managerCinema = userCinemaAssignmentService.getCinemaIdByUserId(actor.getId())
                        .orElseThrow(() -> new ForbiddenException("Quản lý chưa được phân công cụm rạp nào."));
                Long staffCinema = userCinemaAssignmentService.getCinemaIdByUserId(targetUser.getId())
                        .orElse(null);

                if (!Objects.equals(managerCinema, staffCinema)) {
                    throw new ForbiddenException("Quản lý chỉ được quản lý tài khoản Staff thuộc chính rạp của mình.");
                }
            }
        }

        if (request.status() == UserStatus.DISABLED) {
            targetUser.setStatus(UserStatus.DISABLED);
        } else if (request.status() == UserStatus.ACTIVE) {
            activateEmail(targetUser);
        } else if (request.status() == UserStatus.PENDING_VERIFICATION) {
            throw new BadRequestException("Cannot move user back to pending verification");
        }

        String auditDetail = String.format("Status changed to %s by %s", targetUser.getStatus(),
                actor != null ? actor.getEmail() : "SYSTEM");
        auditLogService.record(AuditActionType.UPDATE, "USER_STATUS", targetUser.getId(), auditDetail);

        return toProfile(targetUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccessScopeResponse getAccessScope(Long userId) {
        User user = findById(userId);
        List<String> roles = userRoleService.getRoleNames(userId);
        Long cinemaId = userCinemaAssignmentService.getCinemaIdByUserId(userId).orElse(null);
        return new UserAccessScopeResponse(user.getId(), user.getEmail(), user.getStatus(), roles, cinemaId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse toProfile(User user) {
        List<String> roles = userRoleService.getRoleNames(user.getId());
        Long cinemaId = userCinemaAssignmentService.getCinemaIdByUserId(user.getId()).orElse(null);
        return userMapper.toProfile(user, roles, cinemaId);
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    private void validateUniqueEmailAndPhone(String email, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email đã được sử dụng trong hệ thống.");
        }
        if (phone != null && !phone.isBlank() && userProfileRepository.existsByPhone(phone)) {
            throw new ConflictException("Số điện thoại đã được sử dụng trong hệ thống.");
        }
    }

    private void activateEmail(User user) {
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
    }
}
