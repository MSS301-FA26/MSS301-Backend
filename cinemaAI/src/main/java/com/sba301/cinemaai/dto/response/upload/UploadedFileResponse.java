package com.sba301.cinemaai.dto.response.upload;

public record UploadedFileResponse(
        Long id,
        String url,
        String originalFilename,
        String mimeType,
        long fileSize
) {
}
