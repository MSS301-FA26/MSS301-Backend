package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.upload.UploadedFileResponse;
import com.cinemaai.catalog.service.StorageUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/uploads")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Uploads", description = "Admin Cloudinary upload endpoints")
public class AdminUploadController {

    private final StorageUploadService storageUploadService;

    @PostMapping("/images")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload image to Cloudinary")
    public ApiResponse<UploadedFileResponse> uploadImage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            @RequestParam(defaultValue = "images") String folder
    ) {
        MultipartFile target = file != null ? file : (image != null ? image : avatar);
        if (target == null || target.isEmpty()) {
            throw new com.cinemaai.catalog.exception.BadRequestException("Image file is required");
        }
        return ApiResponse.success(
                storageUploadService.uploadImage(target, folder),
                "Image uploaded successfully"
        );
    }

    @PostMapping("/videos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload video to Cloudinary")
    public ApiResponse<UploadedFileResponse> uploadVideo(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "video", required = false) MultipartFile video,
            @RequestParam(value = "trailer", required = false) MultipartFile trailer,
            @RequestParam(defaultValue = "videos") String folder
    ) {
        MultipartFile target = file != null ? file : (video != null ? video : trailer);
        if (target == null || target.isEmpty()) {
            throw new com.cinemaai.catalog.exception.BadRequestException("Video file is required");
        }
        return ApiResponse.success(
                storageUploadService.uploadVideo(target, folder),
                "Video uploaded successfully"
        );
    }
}
