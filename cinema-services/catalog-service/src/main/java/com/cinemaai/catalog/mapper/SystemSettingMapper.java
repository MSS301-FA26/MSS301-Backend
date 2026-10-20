package com.cinemaai.catalog.mapper;

import com.cinemaai.catalog.dto.response.setting.SystemSettingResponse;
import com.cinemaai.catalog.entity.SystemSetting;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SystemSettingMapper {

    SystemSettingResponse toResponse(SystemSetting setting);
}
