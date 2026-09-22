package com.sba301.cinemaai.dto.request.movie;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MovieEditProposalRequest(@NotNull @Valid MovieUpdateRequest proposed, @NotBlank String reason) {}
