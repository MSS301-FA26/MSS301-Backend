package com.cinemaai.identity.controller;

import com.cinemaai.identity.dto.request.user.AdminStaffCreateRequest;
import com.cinemaai.identity.dto.request.user.AdminUserStatusUpdateRequest;
import com.cinemaai.identity.dto.response.ApiResponse;
import com.cinemaai.identity.dto.response.user.UserProfileResponse;
import com.cinemaai.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Users", description = "Admin user management endpoints - requires ADMIN role")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get all users (Admin)")
    public ApiResponse<List<UserProfileResponse>> getUsers() {
        return ApiResponse.success(userService.getAllUsers());
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID (Admin)")
    public ApiResponse<UserProfileResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.success(userService.getById(userId));
    }

    @PostMapping("/staff")
    @Operation(summary = "Create staff account (Admin)")
    public ApiResponse<UserProfileResponse> createStaff(@Valid @RequestBody AdminStaffCreateRequest request) {
        return ApiResponse.success(userService.createStaff(request), "Staff account created successfully");
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Update user status (Admin)")
    public ApiResponse<UserProfileResponse> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusUpdateRequest request
    ) {
        return ApiResponse.success(userService.updateStatus(userId, request), "User status updated successfully");
    }
}
