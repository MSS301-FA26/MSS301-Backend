package com.cinemaai.identity.controller;

import com.cinemaai.identity.dto.request.user.ChangePasswordRequest;
import com.cinemaai.identity.dto.request.user.UserProfileUpdateRequest;
import com.cinemaai.identity.dto.response.ApiResponse;
import com.cinemaai.identity.dto.response.user.UserProfileResponse;
import com.cinemaai.identity.security.AuthenticatedUser;
import com.cinemaai.identity.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Profile")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> currentUser(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(userService.getProfile(user.email()));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        return ApiResponse.success(userService.updateProfile(user.email(), request), "Profile updated successfully");
    }

    @PostMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(user.email(), request);
        return ApiResponse.success(null, "Password changed successfully");
    }

    @PostMapping("/me/avatar")
    public ApiResponse<UserProfileResponse> updateAvatar(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String avatarUrl
    ) {
        String effectiveUrl = avatarUrl != null && !avatarUrl.isBlank()
                ? avatarUrl
                : (file != null ? "/uploads/avatars/" + user.id() + "_" + file.getOriginalFilename() : null);
        return ApiResponse.success(userService.updateAvatar(user.email(), effectiveUrl), "Avatar updated successfully");
    }
}
