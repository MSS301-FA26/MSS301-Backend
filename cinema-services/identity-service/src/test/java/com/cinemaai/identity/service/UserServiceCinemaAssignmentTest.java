package com.cinemaai.identity.service;

import com.cinemaai.identity.dto.request.user.AdminManagerCreateRequest;
import com.cinemaai.identity.dto.request.user.AdminStaffCreateRequest;
import com.cinemaai.identity.dto.request.user.AdminUserStatusUpdateRequest;
import com.cinemaai.identity.dto.response.user.UserProfileResponse;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.RoleName;
import com.cinemaai.identity.enums.UserStatus;
import com.cinemaai.identity.exception.BadRequestException;
import com.cinemaai.identity.exception.ForbiddenException;
import com.cinemaai.identity.mapper.UserMapper;
import com.cinemaai.identity.repository.UserProfileRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.service.impl.UserServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceCinemaAssignmentTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private UserRoleService userRoleService;
    @Mock
    private UserCinemaAssignmentService userCinemaAssignmentService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditLogService auditLogService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository,
                userProfileRepository,
                userRoleService,
                userCinemaAssignmentService,
                userMapper,
                passwordEncoder,
                auditLogService
        );
    }

    @Test
    void testAdminCreateManager_AssignsSpecifiedCinema() {
        String adminEmail = "admin@cinemaai.com";
        Long cinemaAId = 101L;

        AdminManagerCreateRequest req = new AdminManagerCreateRequest(
                "mgr@cinemaai.com",
                "Password@123",
                "Manager Cinema A",
                "0987654321",
                null,
                cinemaAId
        );

        User adminUser = new User(adminEmail, "hash", "Admin User", "0900000000");

        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.of(adminUser));
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.createManager(req, adminEmail);

        verify(userRoleService).assignRole(any(User.class), eq(RoleName.MANAGER));
        verify(userCinemaAssignmentService).assignCinema(any(User.class), eq(cinemaAId), eq(adminUser));
    }

    @Test
    void testManagerCreateStaff_IgnoresClientCinemaIdAndForcesManagerCinema() {
        String managerEmail = "manager@cinemaai.com";
        Long cinemaAId = 101L;
        Long maliciousCinemaBId = 999L;

        User managerUser = new User(managerEmail, "hash", "Manager A", "0911111111");
        Long managerUserId = 10L;
        org.springframework.test.util.ReflectionTestUtils.setField(managerUser, "id", managerUserId);

        AdminStaffCreateRequest req = new AdminStaffCreateRequest(
                "staff@cinemaai.com",
                "Password@123",
                "Staff Cinema A",
                "0123456789",
                null,
                maliciousCinemaBId // Attacker sends cinema B
        );

        when(userRepository.findByEmail(managerEmail)).thenReturn(Optional.of(managerUser));
        when(userRoleService.getRoleNames(managerUserId)).thenReturn(List.of("MANAGER"));
        when(userCinemaAssignmentService.getCinemaIdByUserId(managerUserId)).thenReturn(Optional.of(cinemaAId));
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.createStaffForActor(req, managerEmail);

        // Verify that backend strictly assigned staff to Cinema A (101L), NOT Cinema B (999L)
        verify(userCinemaAssignmentService).assignCinema(any(User.class), eq(cinemaAId), eq(managerUser));
        verify(userCinemaAssignmentService, never()).assignCinema(any(User.class), eq(maliciousCinemaBId), any());
    }

    @Test
    void testManagerCreateStaff_UnassignedManagerFails() {
        String managerEmail = "manager@cinemaai.com";
        Long managerUserId = 10L;
        User managerUser = new User(managerEmail, "hash", "Manager Unassigned", "0911111111");
        org.springframework.test.util.ReflectionTestUtils.setField(managerUser, "id", managerUserId);

        AdminStaffCreateRequest req = new AdminStaffCreateRequest(
                "staff@cinemaai.com",
                "Password@123",
                "Staff",
                "0123456789",
                null,
                101L
        );

        when(userRepository.findByEmail(managerEmail)).thenReturn(Optional.of(managerUser));
        when(userRoleService.getRoleNames(managerUserId)).thenReturn(List.of("MANAGER"));
        when(userCinemaAssignmentService.getCinemaIdByUserId(managerUserId)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> userService.createStaffForActor(req, managerEmail));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testManagerUpdateStaffStatus_DifferentCinemaBlocked() {
        String managerEmail = "manager@cinemaai.com";
        Long managerUserId = 10L;
        User managerUser = new User(managerEmail, "hash", "Manager A", "0911111111");
        org.springframework.test.util.ReflectionTestUtils.setField(managerUser, "id", managerUserId);

        User staffUser = new User("staffB@cinemaai.com", "hash", "Staff B", "0922222222");
        org.springframework.test.util.ReflectionTestUtils.setField(staffUser, "id", 88L);

        Long managerCinemaId = 101L;
        Long staffCinemaId = 202L; // Different cinema

        when(userRepository.findById(88L)).thenReturn(Optional.of(staffUser));
        when(userRepository.findByEmail(managerEmail)).thenReturn(Optional.of(managerUser));
        when(userRoleService.getRoleNames(managerUserId)).thenReturn(List.of("MANAGER"));
        when(userRoleService.getRoleNames(88L)).thenReturn(List.of("STAFF"));
        when(userCinemaAssignmentService.getCinemaIdByUserId(managerUserId)).thenReturn(Optional.of(managerCinemaId));
        when(userCinemaAssignmentService.getCinemaIdByUserId(88L)).thenReturn(Optional.of(staffCinemaId));

        AdminUserStatusUpdateRequest req = new AdminUserStatusUpdateRequest(UserStatus.DISABLED);

        assertThrows(ForbiddenException.class, () -> userService.updateStatusForActor(88L, req, managerEmail));
    }

    @Test
    void testManagerUpdateStaffStatus_TargetIsManagerBlocked() {
        String managerEmail = "manager@cinemaai.com";
        Long managerUserId = 10L;
        Long targetManagerId = 20L;

        User managerUser = new User(managerEmail, "hash", "Manager A", "0911111111");
        org.springframework.test.util.ReflectionTestUtils.setField(managerUser, "id", managerUserId);

        User targetManager = new User("managerB@cinemaai.com", "hash", "Manager B", "0933333333");
        org.springframework.test.util.ReflectionTestUtils.setField(targetManager, "id", targetManagerId);

        when(userRepository.findById(targetManagerId)).thenReturn(Optional.of(targetManager));
        when(userRepository.findByEmail(managerEmail)).thenReturn(Optional.of(managerUser));
        when(userRoleService.getRoleNames(managerUserId)).thenReturn(List.of("MANAGER"));
        when(userRoleService.getRoleNames(targetManagerId)).thenReturn(List.of("MANAGER"));

        AdminUserStatusUpdateRequest req = new AdminUserStatusUpdateRequest(UserStatus.DISABLED);

        // Manager cannot lock or edit another Manager
        assertThrows(ForbiddenException.class, () -> userService.updateStatusForActor(targetManagerId, req, managerEmail));
    }
}
