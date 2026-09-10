package com.sba301.cinemaai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private String cloudinaryFolderPrefix = "cinema-ai";
    private String defaultImageFolder = "images";
}
