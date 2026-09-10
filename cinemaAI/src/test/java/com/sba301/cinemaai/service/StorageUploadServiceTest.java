package com.sba301.cinemaai.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.sba301.cinemaai.config.CloudinaryCredentials;
import com.sba301.cinemaai.config.UploadProperties;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.repository.UploadedFileRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.service.impl.StorageUploadServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageUploadServiceTest {

    private static final String TEST_CLOUD_NAME = "test-cloud-name";
    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_API_SECRET = "test-api-secret";

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_UPLOAD_FOLDER = "posters";

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @Mock
    private UploadedFileRepository uploadedFileRepository;

    @Mock
    private UserRepository userRepository;

    private StorageUploadService storageUploadService;

    @BeforeEach
    void setUp() {
        storageUploadService = new StorageUploadServiceImpl(
                cloudinary,
                new CloudinaryCredentials(TEST_CLOUD_NAME, TEST_API_KEY, TEST_API_SECRET),
                uploadProperties(),
                uploadedFileRepository,
                userRepository
        );
    }

    @Test
    void shouldTranslateCloudinaryRuntimeFailureToBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "poster.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[] {1}
        );

        User user = new User(
                "admin@example.com",
                "password",
                "Admin",
                "0900111222"
        );

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenThrow(new RuntimeException("Invalid cloud_name " + TEST_CLOUD_NAME));

        assertThatThrownBy(() -> storageUploadService.uploadImage(file, TEST_UPLOAD_FOLDER, TEST_USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cloudinary upload failed: Invalid cloud_name " + TEST_CLOUD_NAME);

        verify(uploadedFileRepository, never()).save(any());
    }

    @Test
    void shouldRejectMissingCloudinaryConfigurationBeforeUpload() {
        storageUploadService = new StorageUploadServiceImpl(
                cloudinary,
                new CloudinaryCredentials("", TEST_API_KEY, TEST_API_SECRET),
                uploadProperties(),
                uploadedFileRepository,
                userRepository
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "poster.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[] {1}
        );

        assertThatThrownBy(() -> storageUploadService.uploadImage(file, TEST_UPLOAD_FOLDER, TEST_USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cloudinary is not configured");

        verify(userRepository, never()).findById(any());
        assertThat(file.getSize()).isEqualTo(1);
    }

    private UploadProperties uploadProperties() {
        UploadProperties properties = new UploadProperties();
        properties.setCloudinaryFolderPrefix("cinema-ai");
        properties.setDefaultImageFolder("images");
        return properties;
    }
}
