package com.cinemaai.identity.controller;

import com.cinemaai.identity.dto.request.user.AdminManagerCreateRequest;
import com.cinemaai.identity.dto.request.user.AdminStaffCreateRequest;
import com.cinemaai.identity.dto.request.user.AdminUserStatusUpdateRequest;
import com.cinemaai.identity.dto.request.user.AssignCinemaRequest;
import com.cinemaai.identity.dto.response.ApiResponse;
import com.cinemaai.identity.dto.response.user.UserProfileResponse;
import com.cinemaai.identity.enums.RoleName;
import com.cinemaai.identity.security.AuthenticatedUser;
import com.cinemaai.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Users", description = "User management endpoints for Admin and Manager")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get users (Admin sees all, Manager sees staff of assigned cinema)")
    public ApiResponse<List<UserProfileResponse>> getUsers(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam(required = false) RoleName role
    ) {
        String actorEmail = actor != null ? actor.email() : null;
        return ApiResponse.success(userService.getAllUsersForActor(role, actorEmail));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    public ApiResponse<UserProfileResponse> getUser(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long userId
    ) {
        String actorEmail = actor != null ? actor.email() : null;
        return ApiResponse.success(userService.getByIdForActor(userId, actorEmail));
    }

    @PostMapping("/manager")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create manager account assigned to a cinema (Admin only)")
    public ApiResponse<UserProfileResponse> createManager(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody AdminManagerCreateRequest request
    ) {
        String actorEmail = actor != null ? actor.email() : null;
        return ApiResponse.success(userService.createManager(request, actorEmail), "Manager account created successfully");
    }

    @PostMapping("/staff")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create staff account (Manager creates for their assigned cinema; Admin specifies cinema)")
    public ApiResponse<UserProfileResponse> createStaff(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody AdminStaffCreateRequest request
    ) {
        String actorEmail = actor != null ? actor.email() : null;
        return ApiResponse.success(userService.createStaffForActor(request, actorEmail), "Staff account created successfully");
    }

    @PutMapping("/{userId}/cinema")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reassign user cinema (Admin only)")
    public ApiResponse<UserProfileResponse> assignCinema(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long userId,
            @Valid @RequestBody AssignCinemaRequest request
    ) {
        String actorEmail = actor != null ? actor.email() : null;
        return ApiResponse.success(userService.assignCinema(userId, request.cinemaId(), actorEmail), "User cinema reassigned successfully");
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update user status (Admin or Manager for their cinema staff)")
    public ApiResponse<UserProfileResponse> updateStatus(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusUpdateRequest request
    ) {
        String actorEmail = actor != null ? actor.email() : null;
        return ApiResponse.success(userService.updateStatusForActor(userId, request, actorEmail), "User status updated successfully");
    }
}
