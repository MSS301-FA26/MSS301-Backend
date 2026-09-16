package com.cinemaai.identity.dto.request.staff;

import com.cinemaai.identity.enums.StaffStatus;
import jakarta.validation.constraints.NotBlank;

public record AdminStaffProfileUpdateRequest(
        @NotBlank(message = "Employee code is required")
        String employeeCode,

        @NotBlank(message = "Position is required")
        String position,

        Long cinemaId,

        StaffStatus status
) {
}
