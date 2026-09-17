package com.cinemaai.identity.service.impl;

import com.cinemaai.identity.dto.request.user.AdminStaffCreateRequest;
import com.cinemaai.identity.dto.request.user.AdminUserStatusUpdateRequest;
import com.cinemaai.identity.dto.request.user.ChangePasswordRequest;
import com.cinemaai.identity.dto.request.user.UserProfileUpdateRequest;
import com.cinemaai.identity.dto.response.user.UserProfileResponse;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.entity.UserProfile;
import com.cinemaai.identity.enums.AuditActionType;
import com.cinemaai.identity.enums.RoleName;
import com.cinemaai.identity.enums.UserStatus;
import com.cinemaai.identity.exception.BadRequestException;
import com.cinemaai.identity.exception.ConflictException;
import com.cinemaai.identity.exception.NotFoundException;
import com.cinemaai.identity.mapper.UserMapper;
import com.cinemaai.identity.repository.UserProfileRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.service.AuditLogService;
import com.cinemaai.identity.service.UserRoleService;
import com.cinemaai.identity.service.UserService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleService userRoleService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

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
    public List<UserProfileResponse> getAllUsers() {
        return getAllUsers(null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getById(Long id) {
        return toProfile(findById(id));
    }

    @Override
    @Transactional
    public UserProfileResponse createStaff(AdminStaffCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }
        if (request.phone() != null && !request.phone().isBlank()
                && userProfileRepository.existsByPhone(request.phone())) {
            throw new ConflictException("Phone already exists");
        }

        User staff = userRepository.save(new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.phone(),
                request.birthYear()
        ));
        activateEmail(staff);
        userRoleService.assignRole(staff, RoleName.STAFF);
        auditLogService.record(AuditActionType.CREATE, "USER", staff.getId(), staff.getEmail());
        return toProfile(staff);
    }

    @Override
    @Transactional
    public UserProfileResponse updateStatus(Long id, AdminUserStatusUpdateRequest request) {
        User user = findById(id);
        if (request.status() == UserStatus.DISABLED) {
            user.setStatus(UserStatus.DISABLED);
        } else if (request.status() == UserStatus.ACTIVE) {
            activateEmail(user);
        } else if (request.status() == UserStatus.PENDING_VERIFICATION) {
            throw new BadRequestException("Cannot move user back to pending verification");
        }
        auditLogService.record(AuditActionType.UPDATE, "USER", user.getId(),
                user.getEmail() + " -> " + user.getStatus());
        return toProfile(user);
    }

    @Override
    public UserProfileResponse toProfile(User user) {
        return userMapper.toProfile(user, userRoleService.getRoleNames(user.getId()));
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    private void activateEmail(User user) {
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
    }
}
