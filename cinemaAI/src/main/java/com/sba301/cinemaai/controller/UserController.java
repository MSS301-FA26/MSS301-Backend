package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.request.user.ChangePasswordRequest;
import com.sba301.cinemaai.dto.response.user.UserProfileResponse;
import com.sba301.cinemaai.dto.request.user.UserProfileUpdateRequest;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.StorageUploadService;
import com.sba301.cinemaai.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Profile")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final StorageUploadService storageUploadService;

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

    @PostMapping("/me/avatar")
    public ApiResponse<UserProfileResponse> updateAvatar(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam MultipartFile file
    ) {
        String avatarUrl = storageUploadService.uploadImage(file, "avatars", user.id()).url();
        return ApiResponse.success(userService.updateAvatar(user.email(), avatarUrl), "Avatar updated successfully");
    }

    @PostMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(user.email(), request);
        return ApiResponse.success(null, "Password changed successfully");
    }
}
