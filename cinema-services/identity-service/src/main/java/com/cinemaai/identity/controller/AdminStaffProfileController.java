package com.cinemaai.identity.controller;

import com.cinemaai.identity.dto.response.ApiResponse;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.entity.UserCinemaAssignment;
import com.cinemaai.identity.enums.RoleName;
import com.cinemaai.identity.repository.UserCinemaAssignmentRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.repository.UserRoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/admin/staff-profiles", "/api/v1/staff/profiles"})
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@Tag(name = "Admin - Staff Profiles", description = "Quản lý hồ sơ nhân viên cho Admin")
public class AdminStaffProfileController {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserCinemaAssignmentRepository userCinemaAssignmentRepository;

    private final Map<Long, Map<String, Object>> profiles = new ConcurrentHashMap<>();

    @GetMapping
    @Operation(summary = "Lấy danh sách hồ sơ nhân viên")
    public ApiResponse<List<Map<String, Object>>> getAll() {
        Long actorCinema = managerCinema();
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                var roles = userRoleRepository.findByUserId(u.getId());
                boolean isStaffOrMgr = roles.stream().anyMatch(r ->
                    r.getRole().getName() == RoleName.STAFF ||
                    r.getRole().getName() == RoleName.MANAGER ||
                    r.getRole().getName() == RoleName.ADMIN
                );
                if (isStaffOrMgr && (actorCinema == null || isStaffAtCinema(u.getId(), actorCinema))) {
                    var assignment = userCinemaAssignmentRepository.findByUserId(u.getId()).orElse(null);
                    String pos = roles.stream().anyMatch(r -> r.getRole().getName() == RoleName.ADMIN) ? "Quản trị viên"
                            : roles.stream().anyMatch(r -> r.getRole().getName() == RoleName.MANAGER) ? "Quản lý rạp"
                            : "Nhân viên rạp";
                    Map<String, Object> p = new HashMap<>(profiles.getOrDefault(u.getId(), Map.of()));
                    p.put("id", u.getId());
                    p.put("userId", u.getId());
                    p.putIfAbsent("employeeCode", "EMP" + String.format("%03d", u.getId()));
                    p.putIfAbsent("position", pos);
                    p.put("status", u.getStatus() != null ? u.getStatus().name() : "ACTIVE");
                    p.put("cinemaId", assignment != null ? assignment.getCinemaId() : null);
                    p.put("email", u.getEmail());
                    p.put("fullName", u.getFullName());
                    p.put("phone", u.getPhone());
                    profiles.put(u.getId(), p);
                    result.add(new HashMap<>(p));
                }
            }
        } catch (Exception ex) {
            throw new com.cinemaai.identity.exception.BadRequestException("Unable to load staff profiles");
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết hồ sơ nhân viên")
    public ApiResponse<Map<String, Object>> getOne(@PathVariable Long id) {
        requireTarget(id);
        Map<String, Object> p = null;
        if (p == null) {
            var assignment = userCinemaAssignmentRepository.findByUserId(id).orElse(null);
            var user = userRepository.findById(id).orElse(null);
            p = new HashMap<>(profiles.getOrDefault(id, Map.of()));
            p.put("id", id);
            p.put("userId", id);
            p.putIfAbsent("employeeCode", "EMP" + String.format("%03d", id));
            p.putIfAbsent("position", "Nhân viên");
            p.put("status", user != null && user.getStatus() != null ? user.getStatus().name() : "ACTIVE");
            p.put("cinemaId", assignment != null ? assignment.getCinemaId() : null);
            if (user != null) {
                p.put("email", user.getEmail());
                p.put("fullName", user.getFullName());
                p.put("phone", user.getPhone());
            }
            profiles.put(id, p);
        }
        return ApiResponse.success(p);
    }

    @PostMapping
    @Operation(summary = "Tạo hồ sơ nhân viên")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Long id;
        try { id = Long.valueOf(String.valueOf(body.get("userId"))); }
        catch (NumberFormatException ex) { throw new com.cinemaai.identity.exception.BadRequestException("Valid userId required"); }
        requireTarget(id);
        validateFields(body, true);
        Map<String, Object> p = new HashMap<>(body);
        p.put("id", id);
        p.put("userId", id);
        profiles.put(id, p);
        return ApiResponse.success(p);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật hồ sơ nhân viên")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireTarget(id);
        validateFields(body, false);
        Map<String, Object> p = profiles.computeIfAbsent(id, k -> new HashMap<>());
        p.putAll(body);
        p.put("id", id);
        if (!p.containsKey("userId")) {
            p.put("userId", id);
        }
        return ApiResponse.success(p);
    }

    private void requireTarget(Long id) {
        Long actorCinema = managerCinema();
        if (id == null || id <= 0 || !userRepository.existsById(id)
                || (actorCinema == null && userRoleRepository.findByUserId(id).stream().noneMatch(r ->
                Set.of(RoleName.ADMIN, RoleName.MANAGER, RoleName.STAFF).contains(r.getRole().getName())))) {
            throw new com.cinemaai.identity.exception.NotFoundException("Staff profile not found");
        }
        if (actorCinema != null && !isStaffAtCinema(id, actorCinema))
            throw new com.cinemaai.identity.exception.ForbiddenException("Staff profile outside assigned cinema");
    }

    private boolean isStaffAtCinema(Long id, Long cinemaId) {
        var roles = userRoleRepository.findByUserId(id);
        return roles.stream().anyMatch(r -> r.getRole().getName() == RoleName.STAFF)
                && roles.stream().noneMatch(r -> r.getRole().getName() == RoleName.ADMIN || r.getRole().getName() == RoleName.MANAGER)
                && userCinemaAssignmentRepository.findByUserId(id)
                .map(a -> cinemaId.equals(a.getCinemaId())).orElse(false);
    }

    private Long managerCinema() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof com.cinemaai.identity.security.AuthenticatedUser actor))
            throw new com.cinemaai.identity.exception.ForbiddenException("Authenticated actor required");
        if (actor.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) return null;
        if (userRoleRepository.findByUserId(actor.id()).stream().noneMatch(r -> r.getRole().getName() == RoleName.MANAGER))
            throw new com.cinemaai.identity.exception.ForbiddenException("Manager role required");
        return userCinemaAssignmentRepository.findByUserId(actor.id())
                .map(a -> a.getCinemaId()).filter(id -> id != null && id > 0)
                .orElseThrow(() -> new com.cinemaai.identity.exception.ForbiddenException("Assigned cinema required"));
    }

    private void validateFields(Map<String, Object> body, boolean creating) {
        for (var entry : body.entrySet()) {
            if (creating && "userId".equals(entry.getKey())) continue;
            if (!Set.of("employeeCode", "position").contains(entry.getKey())
                    || !(entry.getValue() instanceof String value) || value.length() > 200) {
                throw new com.cinemaai.identity.exception.BadRequestException("Unsupported profile field");
            }
        }
    }
}
