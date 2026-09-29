package com.cinemaai.catalog.service.impl;

import com.cinemaai.catalog.dto.request.setting.SystemSettingRequest;
import com.cinemaai.catalog.dto.response.setting.SystemSettingResponse;
import com.cinemaai.catalog.entity.SystemSetting;
import com.cinemaai.catalog.exception.ConflictException;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.repository.SystemSettingRepository;
import com.cinemaai.catalog.service.SystemSettingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getAllSettings() {
        return systemSettingRepository.findAll().stream()
                .map(SystemSettingResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SystemSettingResponse getByKey(String key) {
        return systemSettingRepository.findByConfigKey(key)
                .map(SystemSettingResponse::fromEntity)
                .orElseThrow(() -> new NotFoundException("Cấu hình hệ thống '" + key + "' không tồn tại"));
    }

    @Override
    @Transactional
    public SystemSettingResponse createSetting(SystemSettingRequest request, String actor) {
        String key = request.configKey().trim();
        if (systemSettingRepository.existsByConfigKey(key)) {
            throw new ConflictException("Khóa cấu hình '" + key + "' đã tồn tại.");
        }
        SystemSetting setting = new SystemSetting(key, request.configValue(), request.description(), actor);
        SystemSetting saved = systemSettingRepository.save(setting);
        log.info("Created system setting key='{}' by {}", saved.getConfigKey(), actor);
        return SystemSettingResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public SystemSettingResponse updateSetting(Long id, SystemSettingRequest request, String actor) {
        SystemSetting setting = systemSettingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cấu hình hệ thống ID #" + id + " không tồn tại"));
        String key = request.configKey().trim();
        if (systemSettingRepository.existsByConfigKeyAndIdNot(key, id)) {
            throw new ConflictException("Khóa cấu hình '" + key + "' đã tồn tại.");
        }
        setting.setConfigKey(key);
        setting.setConfigValue(request.configValue());
        setting.setDescription(request.description());
        setting.setUpdatedBy(actor);
        SystemSetting updated = systemSettingRepository.save(setting);
        log.info("Updated system setting key='{}' by {}", updated.getConfigKey(), actor);
        return SystemSettingResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteSetting(Long id) {
        SystemSetting setting = systemSettingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cấu hình hệ thống ID #" + id + " không tồn tại"));
        systemSettingRepository.delete(setting);
        log.info("Deleted system setting id={}", id);
    }
}
