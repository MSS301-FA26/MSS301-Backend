package com.cinemaai.catalog.dto.response.setting;

import com.cinemaai.catalog.entity.SystemSetting;
import java.time.LocalDateTime;

public record SystemSettingResponse(
        Long id,
        String configKey,
        String configValue,
        String description,
        String updatedBy,
        LocalDateTime updatedAt
) {
    public static SystemSettingResponse fromEntity(SystemSetting setting) {
        if (setting == null) return null;
        return new SystemSettingResponse(
                setting.getId(),
                setting.getConfigKey(),
                setting.getConfigValue(),
                setting.getDescription(),
                setting.getUpdatedBy(),
                setting.getUpdatedAt()
        );
    }
}
