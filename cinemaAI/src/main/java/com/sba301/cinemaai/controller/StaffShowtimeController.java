package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.cinema.ShowtimeResponse;
import com.sba301.cinemaai.service.ShowtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/showtimes")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Staff - Showtimes", description = "Staff showtime incident endpoints - requires ADMIN or STAFF role")
public class StaffShowtimeController {

    private final ShowtimeService showtimeService;

    @PostMapping("/{showtimeId}/cancel")
    @Operation(
            summary = "Cancel showtime and trigger automatic refunds (Staff/Admin)",
            description = """
                    Cancels the showtime and automatically processes refunds for PAID/USED bookings.
                    HOLDING/PENDING_PAYMENT bookings are cancelled without refund.
                    """
    )
    public ApiResponse<ShowtimeResponse> cancelShowtime(
            @PathVariable Long showtimeId,
            @RequestParam String reason
    ) {
        return ApiResponse.success(
                showtimeService.cancelShowtime(showtimeId, reason),
                "Showtime cancelled and refund process initiated"
        );
    }
}
