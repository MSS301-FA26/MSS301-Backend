package com.cinemaai.catalog.dto.request.review;

import jakarta.validation.constraints.Size;

public record ModerateReviewRequest(
        @Size(max = 500, message = "Lý do kiểm duyệt tối đa 500 ký tự")
        String reason
) {}
