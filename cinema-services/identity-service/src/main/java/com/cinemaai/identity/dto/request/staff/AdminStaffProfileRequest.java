package com.cinemaai.identity.dto.request.staff;

import com.cinemaai.identity.enums.StaffStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminStaffProfileRequest(
        @NotNull(message = "User id is required")
        Long userId,

        @NotBlank(message = "Employee code is required")
        String employeeCode,

        @NotBlank(message = "Position is required")
        String position,

        Long cinemaId,

        StaffStatus status
) {
}
