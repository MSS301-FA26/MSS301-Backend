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
import com.cinemaai.identity.service.MailService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
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
    private final MailService mailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final ConcurrentHashMap<String, ProvisionOtpRecord> provisionOtpMap = new ConcurrentHashMap<>();

    private record ProvisionOtpRecord(String email, String otp, LocalDateTime expiresAt, boolean verified) {
        ProvisionOtpRecord withVerified(boolean v) { return new ProvisionOtpRecord(email, otp, expiresAt, v); }
    }

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
                    .orElseThrow(() -> new ForbiddenException("Quáº£n lÃ½ chÆ°a Ä‘Æ°á»£c phÃ¢n cÃ´ng cá»¥m ráº¡p nÃ o."));

            List<Long> cinemaUserIds = userCinemaAssignmentService.getUserIdsByCinemaId(managerCinemaId);
            return userRepository.findAllById(cinemaUserIds).stream()
                    .filter(u -> {
                        List<String> uRoles = userRoleService.getRoleNames(u.getId());
                        return uRoles.contains(RoleName.STAFF.name());
                    })
                    .map(this::toProfile)
                    .toList();
        }

        throw new ForbiddenException("Chá»‰ cÃ³ ADMIN hoáº·c MANAGER má»›i cÃ³ quyá»n xem danh sÃ¡ch nhÃ¢n sá»±.");
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
                throw new ForbiddenException("Quáº£n lÃ½ chá»‰ cÃ³ quyá»n xem thÃ´ng tin tÃ i khoáº£n Staff vÃ  KhÃ¡ch hÃ ng.");
            }

            Long managerCinema = userCinemaAssignmentService.getCinemaIdByUserId(actor.getId())
                    .orElseThrow(() -> new ForbiddenException("Quáº£n lÃ½ chÆ°a Ä‘Æ°á»£c phÃ¢n cÃ´ng cá»¥m ráº¡p nÃ o."));
            Long staffCinema = userCinemaAssignmentService.getCinemaIdByUserId(targetUser.getId())
                    .orElse(null);

            if (!java.util.Objects.equals(managerCinema, staffCinema)) {
                throw new ForbiddenException("Quáº£n lÃ½ chá»‰ Ä‘Æ°á»£c xem tÃ i khoáº£n Staff thuá»™c chÃ­nh ráº¡p cá»§a mÃ¬nh.");
            }

            return toProfile(targetUser);
        }

        throw new ForbiddenException("KhÃ´ng cÃ³ quyá»n truy cáº­p thÃ´ng tin tÃ i khoáº£n.");
    }

    @Override
    @Transactional
    public UserProfileResponse createManager(AdminManagerCreateRequest request, String actorEmail) {
        if (request.cinemaId() == null) {
            throw new BadRequestException("Vui lÃ²ng chá»n cá»¥m ráº¡p cho tÃ i khoáº£n Manager.");
        }
        catalogClient.validateActiveCinema(request.cinemaId());

        validateBirthYear18Plus(request.birthYear());
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
                        .orElseThrow(() -> new ForbiddenException("TÃ i khoáº£n Quáº£n lÃ½ chÆ°a Ä‘Æ°á»£c phÃ¢n cÃ´ng cá»¥m ráº¡p nÃ o."));
            } else if (actorRoles.contains(RoleName.ADMIN.name())) {
                targetCinemaId = request.cinemaId();
                if (targetCinemaId == null) {
                    throw new BadRequestException("Admin cáº§n chá»‰ Ä‘á»‹nh cá»¥m ráº¡p khi táº¡o tÃ i khoáº£n nhÃ¢n viÃªn.");
                }
            } else {
                throw new ForbiddenException("Báº¡n khÃ´ng cÃ³ quyá»n táº¡o tÃ i khoáº£n nhÃ¢n viÃªn.");
            }
        } else {
            targetCinemaId = request.cinemaId();
            if (targetCinemaId == null) {
                throw new BadRequestException("Cá»¥m ráº¡p lÃ  báº¯t buá»™c Ä‘á»ƒ táº¡o nhÃ¢n viÃªn.");
            }
        }

        validateBirthYear18Plus(request.birthYear());
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
                    throw new ForbiddenException("Quáº£n lÃ½ chá»‰ cÃ³ quyá»n khÃ³a hoáº·c Ä‘á»•i tráº¡ng thÃ¡i tÃ i khoáº£n Staff.");
                }

                Long managerCinema = userCinemaAssignmentService.getCinemaIdByUserId(actor.getId())
                        .orElseThrow(() -> new ForbiddenException("Quáº£n lÃ½ chÆ°a Ä‘Æ°á»£c phÃ¢n cÃ´ng cá»¥m ráº¡p nÃ o."));
                Long staffCinema = userCinemaAssignmentService.getCinemaIdByUserId(targetUser.getId())
                        .orElse(null);

                if (!Objects.equals(managerCinema, staffCinema)) {
                    throw new ForbiddenException("Quáº£n lÃ½ chá»‰ Ä‘Æ°á»£c quáº£n lÃ½ tÃ i khoáº£n Staff thuá»™c chÃ­nh ráº¡p cá»§a mÃ¬nh.");
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
            throw new ConflictException("Email Ä‘Ã£ Ä‘Æ°á»£c sá»­ dá»¥ng trong há»‡ thá»‘ng.");
        }
        if (phone != null && !phone.isBlank() && userProfileRepository.existsByPhone(phone)) {
            throw new ConflictException("Sá»‘ Ä‘iá»‡n thoáº¡i Ä‘Ã£ Ä‘Æ°á»£c sá»­ dá»¥ng trong há»‡ thá»‘ng.");
        }
    }

    private void activateEmail(User user) {
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
    }
    private void validateBirthYear18Plus(Integer birthYear) {
        if (birthYear == null) {
            throw new BadRequestException("Năm sinh là bắt buộc.");
        }
        int currentYear = java.time.Year.now().getValue();
        int maxBirthYear = currentYear - 18;
        if (birthYear < 1900 || birthYear > currentYear) {
            throw new BadRequestException("Năm sinh không hợp lệ.");
        }
        if (birthYear > maxBirthYear) {
            int age = currentYear - birthYear;
            throw new BadRequestException("Nhân sự phải đủ ít nhất 18 tuổi (sinh năm " + maxBirthYear + " trở về trước, hiện tại " + age + " tuổi).");
        }
    }

    @Override
    public void sendProvisioningOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email không được để trống.");
        }
        String cleanEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new ConflictException("Email đã được sử dụng bởi một tài khoản khác trong hệ thống.");
        }
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);
        provisionOtpMap.put(cleanEmail, new ProvisionOtpRecord(cleanEmail, otp, expiresAt, false));
        log.info("========== [PROVISION OTP] Email: {}, OTP: {} ==========", cleanEmail, otp);
        if (mailService != null) {
            try {
                mailService.sendOtp(cleanEmail, otp, "Xác minh cấp tài khoản");
            } catch (Exception e) {
                log.warn("Không thể gửi email OTP qua SMTP: {}", e.getMessage());
            }
        }
    }

    @Override
    public boolean verifyProvisioningOtp(String email, String otp) {
        if (email == null || otp == null) {
            throw new BadRequestException("Email và mã OTP là bắt buộc.");
        }
        String cleanEmail = email.trim().toLowerCase();
        ProvisionOtpRecord record = provisionOtpMap.get(cleanEmail);
        if (record == null || record.expiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng gửi lại.");
        }
        if (!record.otp().equals(otp.trim())) {
            throw new BadRequestException("Mã OTP không chính xác.");
        }
        provisionOtpMap.put(cleanEmail, record.withVerified(true));
        return true;
    }
}
