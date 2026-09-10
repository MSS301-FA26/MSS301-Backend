package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.response.upload.UploadedFileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface StorageUploadService {

        public UploadedFileResponse uploadImage(MultipartFile file, String requestedFolder, Long userId);

        public UploadedFileResponse uploadVideo(MultipartFile file, String requestedFolder, Long userId);
}
