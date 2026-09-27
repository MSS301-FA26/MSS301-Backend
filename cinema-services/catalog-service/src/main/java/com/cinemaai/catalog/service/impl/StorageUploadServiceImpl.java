package com.cinemaai.catalog.service.impl;

import com.cinemaai.catalog.dto.response.upload.UploadedFileResponse;
import com.cinemaai.catalog.exception.BadRequestException;
import com.cinemaai.catalog.service.StorageUploadService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StorageUploadServiceImpl implements StorageUploadService {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_VIDEO_SIZE = 100L * 1024 * 1024; // 100MB
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

    @Override
    public UploadedFileResponse uploadImage(MultipartFile file, String requestedFolder) {
        validateImage(file);
        String folder = normalizeFolder(requestedFolder, "images");
        return uploadToCloudinary(file, folder, "image");
    }

    @Override
    public UploadedFileResponse uploadVideo(MultipartFile file, String requestedFolder) {
        validateVideo(file);
        String folder = normalizeFolder(requestedFolder, "videos");
        return uploadToCloudinary(file, folder, "video");
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BadRequestException("Image file must not exceed 10 MB");
        }
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        boolean validMime = contentType != null && (
                ALLOWED_IMAGE_TYPES.contains(contentType) ||
                contentType.startsWith("image/") ||
                "application/octet-stream".equalsIgnoreCase(contentType)
        );
        boolean validExt = filename.endsWith(".jpg") || filename.endsWith(".jpeg") ||
                filename.endsWith(".png") || filename.endsWith(".webp") || filename.endsWith(".gif") ||
                filename.endsWith(".bmp") || filename.endsWith(".svg");

        if (!validMime && !validExt) {
            throw new BadRequestException("Only JPG, PNG, WEBP, and GIF images are allowed");
        }
    }

    private void validateVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Video file is required");
        }
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new BadRequestException("Video file must not exceed 100 MB");
        }
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        boolean validMime = contentType != null && (
                ALLOWED_VIDEO_TYPES.contains(contentType) ||
                contentType.startsWith("video/") ||
                "application/octet-stream".equalsIgnoreCase(contentType)
        );
        boolean validExt = filename.endsWith(".mp4") || filename.endsWith(".webm") ||
                filename.endsWith(".mov") || filename.endsWith(".mkv") || filename.endsWith(".avi") ||
                filename.endsWith(".flv") || filename.endsWith(".wmv") || filename.endsWith(".m4v") ||
                filename.endsWith(".3gp") || filename.endsWith(".ts") || filename.endsWith(".ogv");

        if (!validMime && !validExt) {
            throw new BadRequestException("Only MP4, WEBM, MOV, MKV, and AVI videos are allowed");
        }
    }

    private UploadedFileResponse uploadToCloudinary(MultipartFile file, String folder, String resourceType) {
        try {
            Map<String, Object> params = new java.util.HashMap<>();
            params.put("folder", "cinemaai/" + folder);
            params.put("resource_type", resourceType);
            params.put("unique_filename", true);

            byte[] fileBytes = file.getBytes();
            Map<?, ?> result;
            if ("video".equalsIgnoreCase(resourceType) && fileBytes.length > 20_000_000) {
                params.put("chunk_size", 6_000_000);
                result = cloudinary.uploader().uploadLarge(fileBytes, params);
            } else {
                result = cloudinary.uploader().upload(fileBytes, params);
            }

            String url = (String) result.get("secure_url");
            if (url == null || url.isBlank()) {
                url = (String) result.get("url");
            }
            return new UploadedFileResponse(
                    System.currentTimeMillis(),
                    url,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize()
            );
        } catch (IOException | RuntimeException exception) {
            throw new BadRequestException("Tải file lên Cloudinary thất bại: " + exception.getMessage());
        }
    }

    private String normalizeFolder(String folder, String defaultFolder) {
        if (folder == null || folder.isBlank()) {
            return defaultFolder;
        }
        String normalized = folder.trim().toLowerCase().replaceAll("[^a-z0-9-]", "-");
        return normalized.isBlank() ? defaultFolder : normalized;
    }
}
