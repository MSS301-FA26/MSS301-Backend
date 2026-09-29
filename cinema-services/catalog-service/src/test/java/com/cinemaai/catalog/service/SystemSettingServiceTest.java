package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.setting.SystemSettingRequest;
import com.cinemaai.catalog.dto.response.setting.SystemSettingResponse;
import com.cinemaai.catalog.entity.SystemSetting;
import com.cinemaai.catalog.exception.ConflictException;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.repository.SystemSettingRepository;
import com.cinemaai.catalog.service.impl.SystemSettingServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemSettingServiceTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;

    private SystemSettingServiceImpl systemSettingService;

    @BeforeEach
    void setUp() {
        systemSettingService = new SystemSettingServiceImpl(systemSettingRepository);
    }

    @Test
    void createSetting_success() {
        SystemSettingRequest request = new SystemSettingRequest("ticket_holding_minutes", "5", "Holding duration in minutes");
        when(systemSettingRepository.existsByConfigKey("ticket_holding_minutes")).thenReturn(false);
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(invocation -> {
            SystemSetting s = invocation.getArgument(0);
            return new SystemSetting(1L, s.getConfigKey(), s.getConfigValue(), s.getDescription(), s.getUpdatedBy());
        });

        SystemSettingResponse response = systemSettingService.createSetting(request, "admin@test.com");

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("ticket_holding_minutes", response.configKey());
        assertEquals("5", response.configValue());
        assertEquals("admin@test.com", response.updatedBy());
    }

    @Test
    void createSetting_duplicateKey_throwsConflict() {
        SystemSettingRequest request = new SystemSettingRequest("ticket_holding_minutes", "5", "desc");
        when(systemSettingRepository.existsByConfigKey("ticket_holding_minutes")).thenReturn(true);

        assertThrows(ConflictException.class, () -> systemSettingService.createSetting(request, "admin@test.com"));
        verify(systemSettingRepository, never()).save(any());
    }

    @Test
    void updateSetting_success() {
        SystemSetting existing = new SystemSetting(1L, "key", "val1", "desc1", "actor1");
        when(systemSettingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(systemSettingRepository.existsByConfigKeyAndIdNot("key_updated", 1L)).thenReturn(false);
        when(systemSettingRepository.save(any(SystemSetting.class))).thenReturn(existing);

        SystemSettingRequest request = new SystemSettingRequest("key_updated", "val2", "desc2");
        SystemSettingResponse response = systemSettingService.updateSetting(1L, request, "admin2@test.com");

        assertNotNull(response);
        assertEquals("key_updated", existing.getConfigKey());
        assertEquals("val2", existing.getConfigValue());
        assertEquals("admin2@test.com", existing.getUpdatedBy());
    }

    @Test
    void deleteSetting_notFound_throwsException() {
        when(systemSettingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> systemSettingService.deleteSetting(99L));
        verify(systemSettingRepository, never()).delete(any());
    }

    @Test
    void getAllSettings_returnsList() {
        SystemSetting s1 = new SystemSetting(1L, "k1", "v1", "d1", "a1");
        SystemSetting s2 = new SystemSetting(2L, "k2", "v2", "d2", "a2");
        when(systemSettingRepository.findAll()).thenReturn(List.of(s1, s2));

        List<SystemSettingResponse> result = systemSettingService.getAllSettings();
        assertEquals(2, result.size());
        assertEquals("k1", result.get(0).configKey());
        assertEquals("k2", result.get(1).configKey());
    }
}
