package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.staff.AdminStaffProfileRequest;
import com.sba301.cinemaai.dto.request.staff.AdminStaffProfileUpdateRequest;
import com.sba301.cinemaai.dto.response.staff.StaffProfileResponse;
import com.sba301.cinemaai.enums.StaffStatus;
import java.util.List;

public interface StaffProfileService {

    List<StaffProfileResponse> list();

    StaffProfileResponse create(AdminStaffProfileRequest request);

    StaffProfileResponse update(Long profileId, AdminStaffProfileUpdateRequest request);

    StaffProfileResponse updateStatus(Long profileId, StaffStatus status);
}
