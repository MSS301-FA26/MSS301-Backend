package com.cinemaai.identity.controller;

import com.cinemaai.identity.dto.response.ApiResponse;
import com.cinemaai.identity.dto.response.user.UserAccessScopeResponse;
import com.cinemaai.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
@Tag(name = "Internal - Users", description = "Internal cross-service access scope validation")
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{userId}/scope")
    @Operation(summary = "Get user access scope (Internal Service-to-Service)")
    public ApiResponse<UserAccessScopeResponse> getUserAccessScope(@PathVariable Long userId) {
        return ApiResponse.success(userService.getAccessScope(userId));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user profile for internal microservices")
    public ApiResponse<com.cinemaai.identity.dto.response.user.UserProfileResponse> getUserProfileInternal(@PathVariable Long userId) {
        return ApiResponse.success(userService.getById(userId));
    }
}
