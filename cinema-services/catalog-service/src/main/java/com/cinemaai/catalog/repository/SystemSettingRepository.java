package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.SystemSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findByConfigKey(String configKey);

    boolean existsByConfigKey(String configKey);

    boolean existsByConfigKeyAndIdNot(String configKey, Long id);
}
