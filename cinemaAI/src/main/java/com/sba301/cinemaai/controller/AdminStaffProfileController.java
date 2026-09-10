package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.staff.AdminStaffProfileRequest;
import com.sba301.cinemaai.dto.request.staff.AdminStaffProfileUpdateRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.staff.StaffProfileResponse;
import com.sba301.cinemaai.enums.StaffStatus;
import com.sba301.cinemaai.service.StaffProfileService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/staff-profiles")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Staff Profiles", description = "Admin management of staff profiles")
public class AdminStaffProfileController {

    private final StaffProfileService staffProfileService;

    @GetMapping
    @Operation(summary = "List staff profiles (Admin)")
    public ApiResponse<List<StaffProfileResponse>> list() {
        return ApiResponse.success(staffProfileService.list());
    }

    @PostMapping
    @Operation(summary = "Create staff profile (Admin)")
    public ApiResponse<StaffProfileResponse> create(@Valid @RequestBody AdminStaffProfileRequest request) {
        return ApiResponse.success(staffProfileService.create(request), "Staff profile created successfully");
    }

    @PutMapping("/{profileId}")
    @Operation(summary = "Update staff profile (Admin)")
    public ApiResponse<StaffProfileResponse> update(
            @PathVariable Long profileId,
            @Valid @RequestBody AdminStaffProfileUpdateRequest request
    ) {
        return ApiResponse.success(staffProfileService.update(profileId, request), "Staff profile updated successfully");
    }

    @PatchMapping("/{profileId}/status")
    @Operation(summary = "Update staff profile status (Admin)")
    public ApiResponse<StaffProfileResponse> updateStatus(
            @PathVariable Long profileId,
            @RequestParam StaffStatus status
    ) {
        return ApiResponse.success(staffProfileService.updateStatus(profileId, status), "Staff profile status updated successfully");
    }
}
