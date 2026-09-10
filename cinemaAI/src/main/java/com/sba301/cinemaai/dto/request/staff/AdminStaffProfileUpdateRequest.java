package com.sba301.cinemaai.dto.request.staff;

import com.sba301.cinemaai.enums.StaffStatus;
import jakarta.validation.constraints.NotBlank;

public record AdminStaffProfileUpdateRequest(
        @NotBlank(message = "Employee code is required")
        String employeeCode,

        @NotBlank(message = "Position is required")
        String position,

        StaffStatus status
) {
}
