package com.cinemaai.catalog.security;

import com.cinemaai.catalog.exception.ForbiddenException;
import com.cinemaai.catalog.exception.UnauthorizedException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.*;

class CinemaSecurityServiceTest {

    private CinemaSecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService = new CinemaSecurityService();
    }

    @Test
    void validateCinemaAccess_adminAllowedAnywhere() {
        AuthenticatedUser admin = new AuthenticatedUser(
                1L, "admin@cinemaai.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                null
        );

        assertDoesNotThrow(() -> securityService.validateCinemaAccess(admin, 1L));
        assertDoesNotThrow(() -> securityService.validateCinemaAccess(admin, 2L));
        assertDoesNotThrow(() -> securityService.validateCinemaAccess(admin, null));
    }

    @Test
    void validateCinemaAccess_managerAllowedOnlyAssignedCinema() {
        AuthenticatedUser manager = new AuthenticatedUser(
                2L, "manager@cinemaai.com",
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")),
                10L
        );

        assertDoesNotThrow(() -> securityService.validateCinemaAccess(manager, 10L));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                securityService.validateCinemaAccess(manager, 99L));
        assertTrue(ex.getMessage().contains("Quản lý không có quyền"));
    }

    @Test
    void validateCinemaAccess_managerWithoutCinemaThrowsForbidden() {
        AuthenticatedUser managerWithoutCinema = new AuthenticatedUser(
                3L, "manager_unassigned@cinemaai.com",
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")),
                null
        );

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                securityService.validateCinemaAccess(managerWithoutCinema, 10L));
        assertTrue(ex.getMessage().contains("chưa được phân công cụm rạp"));
    }

    @Test
    void validateCinemaAccess_unauthenticatedThrowsUnauthorized() {
        assertThrows(UnauthorizedException.class, () ->
                securityService.validateCinemaAccess(null, 10L));
    }

    @Test
    void resolveEnforcedCinemaId_adminCanQueryAnyOrAll() {
        AuthenticatedUser admin = new AuthenticatedUser(
                1L, "admin@cinemaai.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                null
        );

        assertNull(securityService.resolveEnforcedCinemaId(admin, null));
        assertEquals(5L, securityService.resolveEnforcedCinemaId(admin, 5L));
    }

    @Test
    void resolveEnforcedCinemaId_managerForcedToAssignedCinema() {
        AuthenticatedUser manager = new AuthenticatedUser(
                2L, "manager@cinemaai.com",
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")),
                10L
        );

        // When manager passes null, returns assigned cinema
        assertEquals(10L, securityService.resolveEnforcedCinemaId(manager, null));
        // When manager passes matching cinema, returns assigned cinema
        assertEquals(10L, securityService.resolveEnforcedCinemaId(manager, 10L));

        // When manager passes different cinema, throws Forbidden
        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                securityService.resolveEnforcedCinemaId(manager, 20L));
        assertTrue(ex.getMessage().contains("Quản lý không có quyền"));
    }
}
