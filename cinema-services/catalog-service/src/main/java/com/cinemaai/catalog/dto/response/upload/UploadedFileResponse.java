package com.cinemaai.catalog.dto.response.upload;

public record UploadedFileResponse(
        Long id,
        String url,
        String originalFilename,
        String mimeType,
        long fileSize
) {}
