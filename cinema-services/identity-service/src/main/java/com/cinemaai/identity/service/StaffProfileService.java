package com.cinemaai.identity.service;

import com.cinemaai.identity.dto.request.staff.AdminStaffProfileRequest;
import com.cinemaai.identity.dto.request.staff.AdminStaffProfileUpdateRequest;
import com.cinemaai.identity.dto.response.staff.StaffProfileResponse;
import com.cinemaai.identity.enums.StaffStatus;
import java.util.List;

public interface StaffProfileService {

    List<StaffProfileResponse> list();

    StaffProfileResponse create(AdminStaffProfileRequest request);

    StaffProfileResponse update(Long profileId, AdminStaffProfileUpdateRequest request);

    StaffProfileResponse updateStatus(Long profileId, StaffStatus status);
}
