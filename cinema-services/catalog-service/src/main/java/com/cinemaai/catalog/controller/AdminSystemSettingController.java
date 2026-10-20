package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.request.setting.SystemSettingRequest;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.setting.SystemSettingResponse;
import com.cinemaai.catalog.security.AuthenticatedUser;
import com.cinemaai.catalog.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/system-settings")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - System Settings", description = "Global system configuration endpoints - requires ADMIN role")
public class AdminSystemSettingController {

    private final SystemSettingService systemSettingService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "Get all system settings (Admin only)")
    public ApiResponse<List<SystemSettingResponse>> getAllSettings() {
        return ApiResponse.success(systemSettingService.getAllSettings());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{key}")
    @Operation(summary = "Get system setting by key (Admin only)")
    public ApiResponse<SystemSettingResponse> getSettingByKey(@PathVariable String key) {
        return ApiResponse.success(systemSettingService.getByKey(key));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create system setting (Admin only)")
    public ApiResponse<SystemSettingResponse> createSetting(
            @Valid @RequestBody SystemSettingRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        String actor = (user != null && user.email() != null) ? user.email() : "ADMIN";
        return ApiResponse.success(systemSettingService.createSetting(request, actor), "System setting created successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Update system setting (Admin only)")
    public ApiResponse<SystemSettingResponse> updateSetting(
            @PathVariable Long id,
            @Valid @RequestBody SystemSettingRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        String actor = (user != null && user.email() != null) ? user.email() : "ADMIN";
        return ApiResponse.success(systemSettingService.updateSetting(id, request, actor), "System setting updated successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete system setting (Admin only)")
    public ApiResponse<Void> deleteSetting(@PathVariable Long id) {
        systemSettingService.deleteSetting(id);
        return ApiResponse.success(null, "System setting deleted successfully");
    }
}
