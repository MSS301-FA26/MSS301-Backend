package com.sba301.cinemaai.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CloudinaryConfig {

    @Bean
    public CloudinaryCredentials cloudinaryCredentials(
            @Value("${cloudinary.url:}") String cloudinaryUrl,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret
    ) {
        CloudinaryCredentials explicitCredentials = new CloudinaryCredentials(
                firstText(cloudName, ""),
                firstText(apiKey, ""),
                firstText(apiSecret, "")
        );
        Optional<CloudinaryCredentials> localEnvCredentials = readLocalEnvCredentials();
        CloudinaryCredentials credentials;
        if (explicitCredentials.isConfigured()) {
            credentials = localEnvCredentials
                    .filter(localCredentials -> sameCloudName(localCredentials, explicitCredentials))
                    .map(localCredentials -> {
                        log.info("Cloudinary credentials loaded from local .env for cloud_name={}",
                                localCredentials.cloudName());
                        return localCredentials;
                    })
                    .orElse(explicitCredentials);
        } else {
            credentials = parseCloudinaryUrl(cloudinaryUrl)
                    .or(() -> localEnvCredentials)
                    .orElse(explicitCredentials);
        }

        if (credentials.isConfigured()) {
            log.info("Cloudinary configured for cloud_name={} api_key={}",
                    credentials.cloudName(),
                    mask(credentials.apiKey()));
        } else {
            log.warn("Cloudinary is not fully configured");
        }
        return credentials;
    }

    @Bean
    public Cloudinary cloudinary(CloudinaryCredentials credentials) {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", credentials.cloudName(),
                "api_key", credentials.apiKey(),
                "api_secret", credentials.apiSecret(),
                "secure", true
        ));
    }

    private Optional<CloudinaryCredentials> readLocalEnvCredentials() {
        return readLocalEnvCredentials(".")
                .or(() -> readLocalEnvCredentials("BE/cinemaAI"));
    }

    private Optional<CloudinaryCredentials> readLocalEnvCredentials(String directory) {
        Path envPath = Path.of(directory, ".env");
        if (!Files.isRegularFile(envPath)) {
            return Optional.empty();
        }
        Map<String, String> entries = new HashMap<>();
        try {
            for (String line : Files.readAllLines(envPath)) {
                putEnvEntry(entries, line);
            }
        } catch (IOException exception) {
            log.warn("Could not read local .env for Cloudinary configuration: {}", envPath);
            return Optional.empty();
        }
        CloudinaryCredentials credentials = new CloudinaryCredentials(
                firstText(entries.get("CLOUDINARY_CLOUD_NAME"), ""),
                firstText(entries.get("CLOUDINARY_API_KEY"), ""),
                firstText(entries.get("CLOUDINARY_API_SECRET"), "")
        );
        return credentials.isConfigured() ? Optional.of(credentials) : Optional.empty();
    }

    private void putEnvEntry(Map<String, String> entries, String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isBlank() || trimmed.startsWith("#")) {
            return;
        }
        if (trimmed.startsWith("export ")) {
            trimmed = trimmed.substring("export ".length()).trim();
        }
        int separator = trimmed.indexOf('=');
        if (separator <= 0) {
            return;
        }
        String key = trimmed.substring(0, separator).trim();
        if (!key.startsWith("CLOUDINARY_")) {
            return;
        }
        String value = trimmed.substring(separator + 1).trim();
        entries.put(key, stripQuotes(value));
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private boolean sameCloudName(CloudinaryCredentials first, CloudinaryCredentials second) {
        return hasText(first.cloudName())
                && hasText(second.cloudName())
                && first.cloudName().equalsIgnoreCase(second.cloudName());
    }

    private Optional<CloudinaryCredentials> parseCloudinaryUrl(String cloudinaryUrl) {
        if (!hasText(cloudinaryUrl)) {
            return Optional.empty();
        }
        URI uri;
        try {
            uri = URI.create(cloudinaryUrl.trim());
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        String userInfo = uri.getUserInfo();
        String host = uri.getHost();
        if (!"cloudinary".equalsIgnoreCase(uri.getScheme()) || !hasText(userInfo) || !hasText(host)) {
            return Optional.empty();
        }
        String[] parts = userInfo.split(":", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }
        return Optional.of(new CloudinaryCredentials(
                host.trim(),
                decode(parts[0]),
                decode(parts[1])
        ));
    }

    private String firstText(String first, String second) {
        if (hasText(first)) {
            return first.trim();
        }
        if (hasText(second)) {
            return second.trim();
        }
        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8).trim();
    }

    private String mask(String value) {
        if (!hasText(value) || value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }
}
