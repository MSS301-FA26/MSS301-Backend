package com.cinemaai.identity.dto.response.staff;

import com.cinemaai.identity.entity.StaffProfile;

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
                profile.getCinemaId(),
                null
        );
    }

    public static StaffProfileResponse from(StaffProfile profile, String cinemaName) {
        return new StaffProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getEmail(),
                profile.getUser().getFullName(),
                profile.getEmployeeCode(),
                profile.getPosition(),
                profile.getStatus().name(),
                profile.getCinemaId(),
                cinemaName
        );
    }
}
