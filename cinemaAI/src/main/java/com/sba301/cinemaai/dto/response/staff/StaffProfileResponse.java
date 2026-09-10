package com.sba301.cinemaai.dto.response.staff;

import com.sba301.cinemaai.entity.StaffProfile;

public record StaffProfileResponse(
        Long id,
        Long userId,
        String userEmail,
        String userFullName,
        String employeeCode,
        String position,
        String status,
        Long cinemaId,
        String cinemaName
) {
    public static StaffProfileResponse from(StaffProfile profile) {
        return new StaffProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getEmail(),
                profile.getUser().getFullName(),
                profile.getEmployeeCode(),
                profile.getPosition(),
                profile.getStatus().name(),
                profile.getCinema() == null ? null : profile.getCinema().getId(),
                profile.getCinema() == null ? null : profile.getCinema().getName()
        );
    }
}
