package com.cinemaai.identity.service;

import com.cinemaai.identity.client.CatalogClient;
import com.cinemaai.identity.dto.request.user.AdminUserStatusUpdateRequest;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.UserStatus;
import com.cinemaai.identity.mapper.UserMapper;
import com.cinemaai.identity.repository.UserProfileRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.service.impl.UserServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceCustomerActivationTest {
    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserRoleService userRoleService;
    @Mock private UserCinemaAssignmentService userCinemaAssignmentService;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditLogService auditLogService;
    @Mock private CatalogClient catalogClient;
    @Mock private CustomerActivationOutboxService customerActivationOutboxService;
    @InjectMocks private UserServiceImpl userService;

    @Test
    void adminCustomerActivationRecordsTransitionButRepeatedActiveUpdateDoesNot() {
        User customer = user(false, UserStatus.PENDING_VERIFICATION);
        when(userRepository.findById(914L)).thenReturn(Optional.of(customer));
        when(userRoleService.getRoleNames(914L)).thenReturn(List.of("CUSTOMER"));

        userService.updateStatus(914L, new AdminUserStatusUpdateRequest(UserStatus.ACTIVE));
        userService.updateStatus(914L, new AdminUserStatusUpdateRequest(UserStatus.ACTIVE));

        assertTrue(customer.isEmailVerified());
        assertEquals(UserStatus.ACTIVE, customer.getStatus());
        verify(customerActivationOutboxService).recordActivation(customer);
        verifyNoMoreInteractions(customerActivationOutboxService);
    }

    @Test
    void adminReactivationOfVerifiedDisabledCustomerRecordsStatusTransition() {
        User customer = user(true, UserStatus.DISABLED);
        when(userRepository.findById(914L)).thenReturn(Optional.of(customer));

        userService.updateStatus(914L, new AdminUserStatusUpdateRequest(UserStatus.ACTIVE));

        assertEquals(UserStatus.ACTIVE, customer.getStatus());
        assertTrue(customer.isEmailVerified());
        verify(customerActivationOutboxService).recordActivation(customer);
    }

    @Test
    void disablingCustomerDoesNotRecordAnActivation() {
        User customer = user(true, UserStatus.ACTIVE);
        when(userRepository.findById(914L)).thenReturn(Optional.of(customer));

        userService.updateStatus(914L, new AdminUserStatusUpdateRequest(UserStatus.DISABLED));

        assertEquals(UserStatus.DISABLED, customer.getStatus());
        verifyNoInteractions(customerActivationOutboxService);
    }

    @Test
    void alreadyActiveVerifiedCustomerUpdateDoesNotRecordAnActivation() {
        User customer = user(true, UserStatus.ACTIVE);
        when(userRepository.findById(914L)).thenReturn(Optional.of(customer));

        userService.updateStatus(914L, new AdminUserStatusUpdateRequest(UserStatus.ACTIVE));

        verifyNoInteractions(customerActivationOutboxService);
    }

    private static User user(boolean verified, UserStatus status) {
        User user = new User("admin-activated-customer@example.com", "hash", "Customer", null);
        ReflectionTestUtils.setField(user, "id", 914L);
        user.setEmailVerified(verified);
        user.setStatus(status);
        return user;
    }
}
