package com.cinemaai.booking.security;

import com.cinemaai.booking.client.IdentityClient;
import com.cinemaai.booking.client.dto.UserAccessScopeDto;
import com.cinemaai.booking.exception.ForbiddenException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CinemaSecurityServiceTest {

    @Mock
    private IdentityClient identityClient;

    private CinemaSecurityService cinemaSecurityService;

    @BeforeEach
    void setUp() {
        cinemaSecurityService = new CinemaSecurityService(identityClient);
    }

    @Test
    void testValidateCinemaAccess_AdminAllowedAnywhere() {
        AuthenticatedUser admin = new AuthenticatedUser(1L, "admin@test.com", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(identityClient.getUserAccessScope(1L)).thenReturn(
                new UserAccessScopeDto(1L, "admin@test.com", "ACTIVE", List.of("ADMIN"), null)
        );

        assertDoesNotThrow(() -> cinemaSecurityService.validateCinemaAccess(admin, 101L, false));
        assertDoesNotThrow(() -> cinemaSecurityService.validateCinemaAccess(admin, 999L, false));
    }

    @Test
    void testValidateCinemaAccess_ManagerSameCinemaAllowed() {
        AuthenticatedUser manager = new AuthenticatedUser(10L, "mgr@test.com", List.of(new SimpleGrantedAuthority("ROLE_MANAGER")), 101L);
        when(identityClient.getUserAccessScope(10L)).thenReturn(
                new UserAccessScopeDto(10L, "mgr@test.com", "ACTIVE", List.of("MANAGER"), 101L)
        );

        assertDoesNotThrow(() -> cinemaSecurityService.validateCinemaAccess(manager, 101L, false));
    }

    @Test
    void testValidateCinemaAccess_ManagerDifferentCinemaBlocked() {
        AuthenticatedUser manager = new AuthenticatedUser(10L, "mgr@test.com", List.of(new SimpleGrantedAuthority("ROLE_MANAGER")), 101L);
        when(identityClient.getUserAccessScope(10L)).thenReturn(
                new UserAccessScopeDto(10L, "mgr@test.com", "ACTIVE", List.of("MANAGER"), 101L)
        );

        assertThrows(ForbiddenException.class, () -> cinemaSecurityService.validateCinemaAccess(manager, 202L, false));
    }

    @Test
    void testValidateCinemaAccess_StaffBlockedFromAdminOperations() {
        AuthenticatedUser staff = new AuthenticatedUser(20L, "staff@test.com", List.of(new SimpleGrantedAuthority("ROLE_STAFF")), 101L);
        when(identityClient.getUserAccessScope(20L)).thenReturn(
                new UserAccessScopeDto(20L, "staff@test.com", "ACTIVE", List.of("STAFF"), 101L)
        );

        // allowStaff = false (e.g. for cancel, refund, admin reports)
        assertThrows(ForbiddenException.class, () -> cinemaSecurityService.validateCinemaAccess(staff, 101L, false));
    }

    @Test
    void testValidateCinemaAccess_StaffAllowedSameCinemaForCheckIn() {
        AuthenticatedUser staff = new AuthenticatedUser(20L, "staff@test.com", List.of(new SimpleGrantedAuthority("ROLE_STAFF")), 101L);
        when(identityClient.getUserAccessScope(20L)).thenReturn(
                new UserAccessScopeDto(20L, "staff@test.com", "ACTIVE", List.of("STAFF"), 101L)
        );

        // allowStaff = true
        assertDoesNotThrow(() -> cinemaSecurityService.validateCinemaAccess(staff, 101L, true));
    }

    @Test
    void testValidateCinemaAccess_StaffDifferentCinemaBlockedForCheckIn() {
        AuthenticatedUser staff = new AuthenticatedUser(20L, "staff@test.com", List.of(new SimpleGrantedAuthority("ROLE_STAFF")), 101L);
        when(identityClient.getUserAccessScope(20L)).thenReturn(
                new UserAccessScopeDto(20L, "staff@test.com", "ACTIVE", List.of("STAFF"), 101L)
        );

        assertThrows(ForbiddenException.class, () -> cinemaSecurityService.validateCinemaAccess(staff, 202L, true));
    }

    @Test
    void testGetAuthoritativeScope_DisabledAccountBlocked() {
        AuthenticatedUser lockedUser = new AuthenticatedUser(99L, "locked@test.com", List.of(new SimpleGrantedAuthority("ROLE_MANAGER")), 101L);
        // IdentityClient throws ForbiddenException when account status is not ACTIVE
        when(identityClient.getUserAccessScope(99L)).thenThrow(new ForbiddenException("Tài khoản đã bị khóa hoặc chưa kích hoạt"));

        assertThrows(ForbiddenException.class, () -> cinemaSecurityService.getAuthoritativeScope(lockedUser));
    }
}
