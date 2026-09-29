package com.cinemaai.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCancelTicketRequest(
        @NotBlank(message = "Lý do hủy vé không được để trống")
        @Size(max = 255, message = "Lý do hủy vé không được vượt quá 255 ký tự")
        String reason
) {
}
