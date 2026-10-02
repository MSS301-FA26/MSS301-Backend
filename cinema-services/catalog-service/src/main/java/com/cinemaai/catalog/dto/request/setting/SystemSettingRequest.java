package com.cinemaai.catalog.dto.request.setting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SystemSettingRequest(
        @NotBlank(message = "Config key is required")
        @Size(max = 100, message = "Config key must be at most 100 characters")
        String configKey,

        @NotBlank(message = "Config value is required")
        String configValue,

        String description
) {}
