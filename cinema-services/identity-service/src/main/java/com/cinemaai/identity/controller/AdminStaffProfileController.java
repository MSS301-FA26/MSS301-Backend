package com.cinemaai.identity.controller;

import com.cinemaai.identity.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/staff-profiles")
@RequiredArgsConstructor
@Tag(name = "Admin - Staff Profiles", description = "Quản lý hồ sơ nhân viên cho Admin")
public class AdminStaffProfileController {

    private final Map<Long, Map<String, Object>> profiles = new ConcurrentHashMap<>();

    @GetMapping
    @Operation(summary = "Lấy danh sách hồ sơ nhân viên")
    public ApiResponse<List<Map<String, Object>>> getAll() {
        return ApiResponse.success(new ArrayList<>(profiles.values()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết hồ sơ nhân viên")
    public ApiResponse<Map<String, Object>> getOne(@PathVariable Long id) {
        Map<String, Object> p = profiles.get(id);
        if (p == null) {
            p = Map.of("id", id, "employeeCode", "EMP" + id, "position", "Staff", "status", "ACTIVE");
        }
        return ApiResponse.success(p);
    }

    @PostMapping
    @Operation(summary = "Tạo hồ sơ nhân viên")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Long id = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : System.currentTimeMillis();
        Map<String, Object> p = new HashMap<>(body);
        p.put("id", id);
        profiles.put(id, p);
        return ApiResponse.success(p);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật hồ sơ nhân viên")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> p = profiles.computeIfAbsent(id, k -> new HashMap<>());
        p.putAll(body);
        p.put("id", id);
        return ApiResponse.success(p);
    }
}
