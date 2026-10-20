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
@Tag(name = "Admin - Staff Profiles", description = "Quản lý hồ sơ nhân viên cho Admin")
public class AdminStaffProfileController {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserCinemaAssignmentRepository userCinemaAssignmentRepository;

    private final Map<Long, Map<String, Object>> profiles = new ConcurrentHashMap<>();

    @GetMapping
    @Operation(summary = "Lấy danh sách hồ sơ nhân viên")
    public ApiResponse<List<Map<String, Object>>> getAll() {
        try {
            List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                var roles = userRoleRepository.findByUserId(u.getId());
                boolean isStaffOrMgr = roles.stream().anyMatch(r ->
                    r.getRole().getName() == RoleName.STAFF ||
                    r.getRole().getName() == RoleName.MANAGER ||
                    r.getRole().getName() == RoleName.ADMIN
                );
                if (isStaffOrMgr && !profiles.containsKey(u.getId())) {
                    var assignment = userCinemaAssignmentRepository.findByUserId(u.getId()).orElse(null);
                    String pos = roles.stream().anyMatch(r -> r.getRole().getName() == RoleName.ADMIN) ? "Quản trị viên"
                            : roles.stream().anyMatch(r -> r.getRole().getName() == RoleName.MANAGER) ? "Quản lý rạp"
                            : "Nhân viên rạp";
                    Map<String, Object> p = new HashMap<>();
                    p.put("id", u.getId());
                    p.put("userId", u.getId());
                    p.put("employeeCode", "EMP" + String.format("%03d", u.getId()));
                    p.put("position", pos);
                    p.put("status", u.getStatus() != null ? u.getStatus().name() : "ACTIVE");
                    p.put("cinemaId", assignment != null ? assignment.getCinemaId() : null);
                    p.put("email", u.getEmail());
                    p.put("fullName", u.getFullName());
                    p.put("phone", u.getPhone());
                    profiles.put(u.getId(), p);
                }
            }
        } catch (Exception ignored) {
        }
        return ApiResponse.success(new ArrayList<>(profiles.values()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết hồ sơ nhân viên")
    public ApiResponse<Map<String, Object>> getOne(@PathVariable Long id) {
        Map<String, Object> p = profiles.get(id);
        if (p == null) {
            var assignment = userCinemaAssignmentRepository.findByUserId(id).orElse(null);
            var user = userRepository.findById(id).orElse(null);
            p = new HashMap<>();
            p.put("id", id);
            p.put("userId", id);
            p.put("employeeCode", "EMP" + String.format("%03d", id));
            p.put("position", "Nhân viên");
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
        Long id = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : System.currentTimeMillis();
        Map<String, Object> p = new HashMap<>(body);
        p.put("id", id);
        p.put("userId", id);
        profiles.put(id, p);
        return ApiResponse.success(p);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật hồ sơ nhân viên")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> p = profiles.computeIfAbsent(id, k -> new HashMap<>());
        p.putAll(body);
        p.put("id", id);
        if (!p.containsKey("userId")) {
            p.put("userId", id);
        }
        return ApiResponse.success(p);
    }
}
