package com.sba301.cinemaai.service.impl;


import com.sba301.cinemaai.config.UploadProperties;
import com.sba301.cinemaai.service.StorageUploadService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sba301.cinemaai.config.CloudinaryCredentials;
import com.sba301.cinemaai.dto.response.upload.UploadedFileResponse;
import com.sba301.cinemaai.entity.UploadedFile;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.UploadedFileRepository;
import com.sba301.cinemaai.repository.UserRepository;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StorageUploadServiceImpl implements StorageUploadService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 100L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4",
            "video/webm",
            "video/quicktime"
    );

    private final Cloudinary cloudinary;
    private final CloudinaryCredentials cloudinaryCredentials;
    private final UploadProperties uploadProperties;
    private final UploadedFileRepository uploadedFileRepository;
    private final UserRepository userRepository;

    @Transactional
    public UploadedFileResponse uploadImage(MultipartFile file, String requestedFolder, Long userId) {
        validateImage(file);
        validateCloudinaryConfiguration();
        String folder = normalizeFolder(requestedFolder);
        User uploadedBy = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return saveUpload(file, folder, "image", uploadedBy);
    }

    @Transactional
    public UploadedFileResponse uploadVideo(MultipartFile file, String requestedFolder, Long userId) {
        validateVideo(file);
        validateCloudinaryConfiguration();
        String folder = normalizeFolder(requestedFolder);
        User uploadedBy = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return saveUpload(file, folder, "video", uploadedBy);
    }

    private UploadedFileResponse saveUpload(MultipartFile file, String folder, String resourceType, User uploadedBy) {
        Map<?, ?> result = uploadToCloudinary(file, folder, resourceType);
        String url = String.valueOf(result.get("secure_url"));
        String publicId = String.valueOf(result.get("public_id"));
        UploadedFile savedFile = uploadedFileRepository.save(new UploadedFile(
                file.getOriginalFilename(),
                publicId,
                "CLOUDINARY",
                url,
                file.getContentType(),
                file.getSize(),
                uploadedBy
        ));
        return toResponse(savedFile);
    }

    private void validateVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Video file is required");
        }
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new BadRequestException("Video file must not exceed 100 MB");
        }
        if (!ALLOWED_VIDEO_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Only MP4, WEBM, and MOV videos are allowed");
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BadRequestException("Image file must not exceed 5 MB");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Only JPG, PNG, and WEBP images are allowed");
        }
    }

    private void validateCloudinaryConfiguration() {
        if (!cloudinaryCredentials.isConfigured()) {
            throw new BadRequestException("Cloudinary is not configured");
        }
    }

    private Map<?, ?> uploadToCloudinary(MultipartFile file, String folder, String resourceType) {
        try {
            return cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", uploadProperties.getCloudinaryFolderPrefix() + "/" + folder,
                            "resource_type", resourceType,
                            "unique_filename", true
                    )
            );
        } catch (IOException | RuntimeException exception) {
            throw new BadRequestException("Cloudinary upload failed: " + cloudinaryErrorMessage(exception));
        }
    }

    private String cloudinaryErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "Unknown Cloudinary error";
        }
        return message;
    }

    private String normalizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return uploadProperties.getDefaultImageFolder();
        }
        String normalized = folder.trim().toLowerCase().replaceAll("[^a-z0-9-]", "-");
        return normalized.isBlank() ? uploadProperties.getDefaultImageFolder() : normalized;
    }

    private UploadedFileResponse toResponse(UploadedFile file) {
        return new UploadedFileResponse(
                file.getId(),
                file.getUrl(),
                file.getOriginalFilename(),
                file.getMimeType(),
                file.getFileSize()
        );
    }
}
