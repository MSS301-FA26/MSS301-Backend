package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.staff.AdminStaffProfileRequest;
import com.sba301.cinemaai.dto.request.staff.AdminStaffProfileUpdateRequest;
import com.sba301.cinemaai.dto.response.staff.StaffProfileResponse;
import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.entity.StaffProfile;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.StaffStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.CinemaRepository;
import com.sba301.cinemaai.repository.StaffProfileRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.StaffProfileService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffProfileServiceImpl implements StaffProfileService {

    private final StaffProfileRepository staffProfileRepository;
    private final UserRepository userRepository;
    private final CinemaRepository cinemaRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<StaffProfileResponse> list() {
        return staffProfileRepository.findAll()
                .stream()
                .map(StaffProfileResponse::from)
                .toList();
    }

    @Transactional
    public StaffProfileResponse create(AdminStaffProfileRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.userId()));
        if (staffProfileRepository.findByUser(user).isPresent()) {
            throw new BadRequestException("User already has a staff profile");
        }
        if (staffProfileRepository.findByEmployeeCode(request.employeeCode()).isPresent()) {
            throw new BadRequestException("Employee code already exists: " + request.employeeCode());
        }
        Cinema cinema = cinemaRepository.findFirstByOrderByIdAsc().orElse(null);
        StaffProfile profile = new StaffProfile(user, cinema, request.employeeCode(), request.position());
        if (request.status() != null) {
            profile.setStatus(request.status());
        }
        StaffProfile saved = staffProfileRepository.save(profile);
        auditLogService.record(AuditActionType.CREATE, "STAFF_PROFILE", saved.getId(), saved.getEmployeeCode());
        return StaffProfileResponse.from(saved);
    }

    @Transactional
    public StaffProfileResponse update(Long profileId, AdminStaffProfileUpdateRequest request) {
        StaffProfile profile = findProfile(profileId);
        staffProfileRepository.findByEmployeeCode(request.employeeCode())
                .filter(existing -> !existing.getId().equals(profileId))
                .ifPresent(existing -> {
                    throw new BadRequestException("Employee code already exists: " + request.employeeCode());
                });
        profile.updateDetails(request.employeeCode(), request.position());
        if (request.status() != null) {
            profile.setStatus(request.status());
        }
        StaffProfile saved = staffProfileRepository.save(profile);
        auditLogService.record(AuditActionType.UPDATE, "STAFF_PROFILE", saved.getId(), saved.getEmployeeCode());
        return StaffProfileResponse.from(saved);
    }

    @Transactional
    public StaffProfileResponse updateStatus(Long profileId, StaffStatus status) {
        if (status == null) {
            throw new BadRequestException("Status is required");
        }
        StaffProfile profile = findProfile(profileId);
        profile.setStatus(status);
        StaffProfile saved = staffProfileRepository.save(profile);
        auditLogService.record(AuditActionType.UPDATE, "STAFF_PROFILE", saved.getId(),
                saved.getEmployeeCode() + " -> " + status);
        return StaffProfileResponse.from(saved);
    }

    private StaffProfile findProfile(Long profileId) {
        return staffProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Staff profile not found: " + profileId));
    }
}
