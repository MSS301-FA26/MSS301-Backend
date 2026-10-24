package com.cinemaai.identity.service;

import com.cinemaai.identity.controller.AdminStaffProfileController;
import com.cinemaai.identity.repository.*;
import com.cinemaai.identity.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StaffProfileInputSecurityTest {
    @org.junit.jupiter.api.BeforeEach void adminActor() {
        var actor = mock(com.cinemaai.identity.security.AuthenticatedUser.class);
        org.mockito.Mockito.doReturn(java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))).when(actor).getAuthorities();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor,null,actor.getAuthorities()));
    }
    @org.junit.jupiter.api.AfterEach void clearActor() { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    @Test void metadataSurvivesReadsAndProtectedFieldsAreRejected() {
        var users=mock(UserRepository.class);
        var roles=mock(UserRoleRepository.class);
        var assignments=mock(UserCinemaAssignmentRepository.class);
        var user=mock(com.cinemaai.identity.entity.User.class);
        when(user.getId()).thenReturn(7L);
        when(user.getEmail()).thenReturn("authoritative@test.com");
        var role=mock(com.cinemaai.identity.entity.UserRole.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(role.getRole().getName()).thenReturn(com.cinemaai.identity.enums.RoleName.STAFF);
        when(users.existsById(7L)).thenReturn(true);
        when(users.findById(7L)).thenReturn(java.util.Optional.of(user));
        when(users.findAll()).thenReturn(java.util.List.of(user));
        when(roles.findByUserId(7L)).thenReturn(java.util.List.of(role));
        var controller=new AdminStaffProfileController(users,roles,assignments);
        controller.create(Map.of("userId",7L,"position","Custom"));
        assertEquals("Custom",controller.getOne(7L).data().get("position"));
        controller.update(7L,Map.of("position","Updated"));
        assertEquals("Updated",controller.getAll().data().get(0).get("position"));
        for(String field:java.util.List.of("userId","role","cinemaId","status","email","password")) {
            assertThrows(com.cinemaai.identity.exception.BadRequestException.class,()->controller.update(7L,Map.of(field,"forbidden")));
        }
        assertEquals("authoritative@test.com",controller.getOne(7L).data().get("email"));
        verify(users,never()).save(any());
        verify(assignments,never()).save(any());
    }
    @org.springframework.context.annotation.Configuration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
    static class MethodSecurity {}

    @Test void allStaffProfileActionsRejectUnprivilegedAndMissingActors() {
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.register(MethodSecurity.class);
            context.registerBean(AdminStaffProfileController.class, () -> new AdminStaffProfileController(
                    mock(UserRepository.class), mock(UserRoleRepository.class), mock(UserCinemaAssignmentRepository.class)));
            context.refresh();
            var controller = context.getBean(AdminStaffProfileController.class);
            try {
                for (String role : java.util.List.of("USER", "CUSTOMER", "STAFF")) {
                    var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            "actor", null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+role)));
                    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
                    assertThrows(org.springframework.security.access.AccessDeniedException.class, controller::getAll);
                    assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> controller.getOne(99L));
                    assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> controller.create(Map.of("userId",99L)));
                    assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> controller.update(99L, Map.of()));
                }
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
                assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class, controller::getAll);
            } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
        }
    }
    @Test void missingTargetsCannotBeCreatedOrCached() {
        var users = mock(UserRepository.class);
        var controller = new AdminStaffProfileController(users, mock(UserRoleRepository.class), mock(UserCinemaAssignmentRepository.class));
        assertThrows(NotFoundException.class, () -> controller.create(Map.of("userId", 99L, "role", "ADMIN")));
        assertThrows(NotFoundException.class, () -> controller.update(99L, Map.of("status", "ACTIVE")));
        assertThrows(NotFoundException.class, () -> controller.getOne(99L));
        assertThrows(NotFoundException.class, () -> controller.getOne(-1L));
    }
}
