package com.cinemaai.identity.service.impl;

import com.cinemaai.identity.dto.request.staff.AdminStaffProfileRequest;
import com.cinemaai.identity.dto.request.staff.AdminStaffProfileUpdateRequest;
import com.cinemaai.identity.dto.response.staff.StaffProfileResponse;
import com.cinemaai.identity.entity.StaffProfile;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.AuditActionType;
import com.cinemaai.identity.enums.StaffStatus;
import com.cinemaai.identity.exception.BadRequestException;
import com.cinemaai.identity.exception.NotFoundException;
import com.cinemaai.identity.repository.StaffProfileRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.service.AuditLogService;
import com.cinemaai.identity.service.StaffProfileService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffProfileServiceImpl implements StaffProfileService {

    private final StaffProfileRepository staffProfileRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<StaffProfileResponse> list() {
        return staffProfileRepository.findAll()
                .stream()
                .map(StaffProfileResponse::from)
                .toList();
    }

    @Override
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

        StaffProfile profile = new StaffProfile(user, request.cinemaId(), request.employeeCode(), request.position());
        if (request.status() != null) {
            profile.setStatus(request.status());
        }
        StaffProfile saved = staffProfileRepository.save(profile);
        auditLogService.record(AuditActionType.CREATE, "STAFF_PROFILE", saved.getId(), saved.getEmployeeCode());
        return StaffProfileResponse.from(saved);
    }

    @Override
    @Transactional
    public StaffProfileResponse update(Long profileId, AdminStaffProfileUpdateRequest request) {
        StaffProfile profile = findProfile(profileId);
        staffProfileRepository.findByEmployeeCode(request.employeeCode())
                .filter(existing -> !existing.getId().equals(profileId))
                .ifPresent(existing -> {
                    throw new BadRequestException("Employee code already exists: " + request.employeeCode());
                });

        profile.updateDetails(request.employeeCode(), request.position());
        if (request.cinemaId() != null) {
            profile.setCinemaId(request.cinemaId());
        }
        if (request.status() != null) {
            profile.setStatus(request.status());
        }
        StaffProfile saved = staffProfileRepository.save(profile);
        auditLogService.record(AuditActionType.UPDATE, "STAFF_PROFILE", saved.getId(), saved.getEmployeeCode());
        return StaffProfileResponse.from(saved);
    }

    @Override
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
