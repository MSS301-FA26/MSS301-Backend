package com.cinemaai.booking.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserProfileDto(
        Long id,
        String email,
        String username,
        String fullName,
        String phone
) {
}
