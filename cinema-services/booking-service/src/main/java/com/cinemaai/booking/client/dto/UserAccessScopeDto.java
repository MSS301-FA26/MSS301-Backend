package com.cinemaai.booking.client.dto;

import java.util.List;

public record UserAccessScopeDto(
        Long userId,
        String email,
        String status,
        List<String> roles,
        Long cinemaId
) {
}
