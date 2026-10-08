package com.cinemaai.catalog.dto.request.movie;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DirectorRequest(
        @NotBlank(message = "Tên đạo diễn không được để trống")
        @Size(max = 255, message = "Tên đạo diễn không được vượt quá 255 ký tự")
        String name,

        String biography,

        @Size(max = 500, message = "URL ảnh không được vượt quá 500 ký tự")
        String avatarUrl
) {}
