package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.request.user.AdminStaffCreateRequest;
import com.sba301.cinemaai.dto.request.user.AdminManagerCreateRequest;
import com.sba301.cinemaai.dto.request.user.ManagerCinemaAssignmentRequest;
import com.sba301.cinemaai.dto.request.user.AdminUserStatusUpdateRequest;
import com.sba301.cinemaai.dto.response.user.UserProfileResponse;
import com.sba301.cinemaai.dto.response.cinema.CinemaResponse;
import com.sba301.cinemaai.enums.RoleName;
import com.sba301.cinemaai.service.UserService;
import com.sba301.cinemaai.service.ManagerAccountService;
import com.sba301.cinemaai.service.CinemaService;
import com.sba301.cinemaai.repository.CinemaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Users", description = "Admin user management endpoints - requires ADMIN role")
public class AdminUserController {

    private final UserService userService;
    private final ManagerAccountService managerAccountService;
    private final CinemaRepository cinemaRepository;
    private final CinemaService cinemaService;

    @GetMapping("/managers/cinemas")
    @Operation(summary = "List cinemas available for manager assignment (Admin only)")
    public ApiResponse<List<CinemaResponse>> assignmentCinemas() {
        return ApiResponse.success(cinemaRepository.findAll().stream()
                .map(cinema -> cinemaService.getCinema(cinema.getId())).toList());
    }

    @PostMapping("/managers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create manager with assigned cinemas (Admin only)")
    public ApiResponse<UserProfileResponse> createManager(@Valid @RequestBody AdminManagerCreateRequest request) {
        return ApiResponse.success(managerAccountService.create(request), "Manager created");
    }

    @GetMapping("/managers/{userId}/cinemas")
    @Operation(summary = "Get manager cinema assignments (Admin only)")
    public ApiResponse<List<Long>> managerCinemas(@PathVariable Long userId) {
        return ApiResponse.success(managerAccountService.assignedCinemaIds(userId));
    }

    @PatchMapping("/managers/{userId}/cinemas")
    @Operation(summary = "Replace manager cinema assignments (Admin only)")
    public ApiResponse<List<Long>> assignManagerCinemas(@PathVariable Long userId,
            @Valid @RequestBody ManagerCinemaAssignmentRequest request) {
        return ApiResponse.success(managerAccountService.assignCinemas(userId, request.cinemaIds()));
    }

    @GetMapping
    @Operation(summary = "Get all users (Admin)", description = "Get all users, optionally filtered by role (e.g. role=STAFF)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    public ApiResponse<List<UserProfileResponse>> getUsers(@RequestParam(required = false) RoleName role) {
        return ApiResponse.success(userService.getAllUsers(role));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID (Admin)", description = "Get a specific user by ID (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ApiResponse<UserProfileResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.success(userService.getById(userId));
    }

    @PostMapping("/staff")
    @Operation(
            summary = "Create staff account (Admin)",
            description = "Create an ACTIVE staff account. The created account receives STAFF role by default and can log in with the provided password."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Staff account created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email or phone already exists")
    })
    public ApiResponse<UserProfileResponse> createStaff(@Valid @RequestBody AdminStaffCreateRequest request) {
        return ApiResponse.success(userService.createStaff(request), "Staff account created successfully");
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Update user status (Admin)", description = "Update the status of a user (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ApiResponse<UserProfileResponse> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusUpdateRequest request
    ) {
        return ApiResponse.success(userService.updateStatus(userId, request), "User status updated successfully");
    }
}
