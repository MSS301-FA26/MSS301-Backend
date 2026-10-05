package com.cinemaai.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "system_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SystemSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    @Setter
    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String description;

    @Setter
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    public SystemSetting(String configKey, String configValue, String description, String updatedBy) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
        this.updatedBy = updatedBy;
    }

    public SystemSetting(Long id, String configKey, String configValue, String description, String updatedBy) {
        this.id = id;
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
        this.updatedBy = updatedBy;
    }
}
