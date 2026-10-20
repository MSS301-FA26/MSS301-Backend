package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.setting.SystemSettingRequest;
import com.cinemaai.catalog.dto.response.setting.SystemSettingResponse;
import java.util.List;

public interface SystemSettingService {

    List<SystemSettingResponse> getAllSettings();

    SystemSettingResponse getByKey(String key);

    SystemSettingResponse createSetting(SystemSettingRequest request, String actor);

    SystemSettingResponse updateSetting(Long id, SystemSettingRequest request, String actor);

    void deleteSetting(Long id);
}
