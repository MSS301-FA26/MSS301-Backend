package com.sba301.cinemaai.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminManagerCreateRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8) @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$") String password,
        @NotBlank String fullName,
        @Pattern(regexp = "^\\+?[0-9]{10,15}$") String phone,
        @NotEmpty List<Long> cinemaIds
) {}
