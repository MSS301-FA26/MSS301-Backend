package com.sba301.cinemaai.dto.request.user;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ManagerCinemaAssignmentRequest(@NotEmpty List<Long> cinemaIds) {}
