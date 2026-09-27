package com.sba301.cinemaai.dto.request.movie;

import jakarta.validation.constraints.NotBlank;

public record MovieRejectRequest(
        @NotBlank(message = "Lý do từ chối không được để trống")
        String reason
) {
}
