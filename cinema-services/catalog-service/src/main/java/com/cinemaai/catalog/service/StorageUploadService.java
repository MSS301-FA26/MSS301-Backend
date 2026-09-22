package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.response.upload.UploadedFileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface StorageUploadService {

    UploadedFileResponse uploadImage(MultipartFile file, String requestedFolder);

    UploadedFileResponse uploadVideo(MultipartFile file, String requestedFolder);
}
