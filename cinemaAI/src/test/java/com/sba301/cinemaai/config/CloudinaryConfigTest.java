package com.sba301.cinemaai.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryConfigTest {

    private final CloudinaryConfig config = new CloudinaryConfig();

    @Test
    void shouldPreferExplicitCredentialsOverCloudinaryUrl() {
        CloudinaryCredentials credentials = config.cloudinaryCredentials(
                "cloudinary://old-key:old-secret@old-cloud",
                "new-cloud",
                "new-key",
                "new-secret"
        );

        assertThat(credentials.cloudName()).isEqualTo("new-cloud");
        assertThat(credentials.apiKey()).isEqualTo("new-key");
        assertThat(credentials.apiSecret()).isEqualTo("new-secret");
    }

    @Test
    void shouldUseCloudinaryUrlWhenExplicitCredentialsAreIncomplete() {
        CloudinaryCredentials credentials = config.cloudinaryCredentials(
                "cloudinary://url-key:url-secret@url-cloud",
                "",
                "",
                ""
        );

        assertThat(credentials.cloudName()).isEqualTo("url-cloud");
        assertThat(credentials.apiKey()).isEqualTo("url-key");
        assertThat(credentials.apiSecret()).isEqualTo("url-secret");
    }
}
