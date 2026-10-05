package com.cinemaai.catalog.dto.request.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @Min(value = 1, message = "Điểm đánh giá tối thiểu là 1")
        @Max(value = 10, message = "Điểm đánh giá tối đa là 10")
        int rating,

        @NotBlank(message = "Nội dung nhận xét không được để trống")
        @Size(min = 5, max = 2000, message = "Nội dung nhận xét phải từ 5 đến 2000 ký tự")
        String content,

        boolean containsSpoiler
) {}
