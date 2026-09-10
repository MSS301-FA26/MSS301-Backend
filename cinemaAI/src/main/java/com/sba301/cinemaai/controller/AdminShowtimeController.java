package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.cinema.BulkShowtimeRequest;
import com.sba301.cinemaai.dto.request.cinema.ShowtimeRequest;
import com.sba301.cinemaai.dto.request.refund.CancelShowtimeRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import java.util.List;
import com.sba301.cinemaai.dto.response.cinema.ShowtimeResponse;
import com.sba301.cinemaai.dto.response.cinema.ShowtimeSeatMapResponse;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.refund.BulkRefundResponse;
import com.sba301.cinemaai.enums.ShowtimeStatus;
import com.sba301.cinemaai.service.ShowtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/showtimes")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Showtimes", description = "Admin showtime management endpoints - requires ADMIN role")
public class AdminShowtimeController {

    private final ShowtimeService showtimeService;

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @GetMapping("/available-slots")
    @Operation(
            summary = "Suggest free time slots (Admin)",
            description = "Lists free start/end time slots for a room on a given date, sized to the movie duration plus cleanup time. Slots step every 15 minutes within operating hours (08:00-23:59)."
    )
    public ApiResponse<List<com.sba301.cinemaai.dto.response.cinema.AvailableSlotResponse>> availableSlots(
            @RequestParam Long roomId,
            @RequestParam Long movieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(showtimeService.getAvailableSlots(roomId, movieId, date));
    }

    @GetMapping
    @Operation(
            summary = "Search showtimes with paging (Admin)",
            description = """
                    Search showtimes with optional filters. All parameters are optional.
                    - **date**: filter to a single calendar day. Omit to search the next 12 months.
                    - **status**: one of SCHEDULED | OPEN | CANCELLED | COMPLETED
                    - **page** / **size**: zero-based page number and page size (default 0 / 20).
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Showtimes retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    public ApiResponse<PageResponse<ShowtimeResponse>> searchShowtimes(
            @Parameter(description = "Filter by movie ID") @RequestParam(required = false) Long movieId,
            @Parameter(description = "Filter by room ID")  @RequestParam(required = false) Long roomId,
            @Parameter(description = "Filter by cinema ID") @RequestParam(required = false) Long cinemaId,
            @Parameter(description = "Filter by showtime status") @RequestParam(required = false) ShowtimeStatus status,
            @Parameter(description = "Filter by date (ISO format: yyyy-MM-dd)")
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Page size (max 100)")   @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(showtimeService.searchAdmin(movieId, roomId, cinemaId, status, date, page, size));
    }

    @GetMapping("/{showtimeId}")
    @Operation(
            summary = "Get showtime by ID (Admin)",
            description = "Retrieve a single showtime by its ID regardless of status (Admin only)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Showtime retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Showtime not found")
    })
    public ApiResponse<ShowtimeResponse> getShowtime(@PathVariable Long showtimeId) {
        return ApiResponse.success(showtimeService.getAdmin(showtimeId));
    }

    @GetMapping("/{showtimeId}/seat-map")
    @Operation(
            summary = "Get showtime seat map (Admin)",
            description = "Returns the seat map for a showtime with real-time booking status (Admin only)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seat map retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Showtime not found")
    })
    public ApiResponse<ShowtimeSeatMapResponse> getSeatMap(@PathVariable Long showtimeId) {
        return ApiResponse.success(showtimeService.getSeatMap(showtimeId));
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create new showtime (Admin)",
            description = """
                    Create a new showtime. Business rules enforced:
                    - Movie must be ACTIVE.
                    - Room must be ACTIVE.
                    - Start time must be in the future.
                    - The room must have no overlapping non-cancelled showtimes.
                    - Initial status can only be SCHEDULED or OPEN (defaults to SCHEDULED).
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Showtime created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or business rule violation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie or room not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - room already has an overlapping showtime")
    })
    public ApiResponse<ShowtimeResponse> createShowtime(@Valid @RequestBody ShowtimeRequest request) {
        return ApiResponse.success(showtimeService.create(request), "Showtime created successfully");
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Bulk create showtimes for one movie (Admin)",
            description = """
                    Create multiple showtime slots for the **same movie** in a single request.
                    All slots share the same `movieId`, `basePrice`, `vipPrice`, `couplePrice`,
                    and `defaultStatus`. Each slot only needs its own `roomId` and `startTime`.
                    An optional per-slot `status` overrides `defaultStatus` for that slot.

                    **All slots are saved in one transaction** — if any slot fails validation
                    the entire batch is rolled back and no showtimes are created.

                    Business rules (applied per slot):
                    - Movie must be ACTIVE.
                    - Room must be ACTIVE.
                    - Start time must be in the future.
                    - Room must have no overlapping non-cancelled showtimes.
                    - Status must be SCHEDULED or OPEN.

                    Example body:
                    ```json
                    {
                      "movieId": 5,
                      "basePrice": 90000,
                      "vipPrice": 130000,
                      "couplePrice": 160000,
                      "defaultStatus": "OPEN",
                      "slots": [
                        { "roomId": 1, "startTime": "2026-06-10T09:00:00" },
                        { "roomId": 1, "startTime": "2026-06-10T14:30:00" },
                        { "roomId": 2, "startTime": "2026-06-10T19:00:00", "status": "SCHEDULED" }
                      ]
                    }
                    ```
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "All showtimes created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or business rule violation in one of the slots"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie or one of the rooms not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - one of the slots overlaps an existing showtime")
    })
    public ApiResponse<List<ShowtimeResponse>> createBulkShowtimes(
            @Valid @RequestBody BulkShowtimeRequest request) {
        return ApiResponse.success(showtimeService.createBulk(request),
                request.slots().size() + " showtime(s) created successfully");
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @PutMapping("/{showtimeId}")
    @Operation(
            summary = "Update showtime (Admin)",
            description = """
                    Update movie, room, time, and prices for an existing showtime.
                    - Cannot update a CANCELLED or COMPLETED showtime.
                    - Cannot update if the showtime has active bookings (HOLDING / PENDING_PAYMENT / PAID / REFUND_REQUESTED).
                    - The room must have no overlapping non-cancelled showtimes (excluding this one).
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Showtime updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or business rule violation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Showtime, movie or room not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - showtime has active bookings or overlapping room schedule")
    })
    public ApiResponse<ShowtimeResponse> updateShowtime(
            @PathVariable Long showtimeId,
            @Valid @RequestBody ShowtimeRequest request
    ) {
        return ApiResponse.success(showtimeService.update(showtimeId, request), "Showtime updated successfully");
    }

    @PatchMapping("/{showtimeId}/status")
    @Operation(
            summary = "Update showtime status (Admin)",
            description = """
                    Change the status of a showtime. Allowed transitions:
                    - **SCHEDULED → OPEN** or **SCHEDULED → CANCELLED**
                    - **OPEN → COMPLETED** or **OPEN → CANCELLED**
                    - CANCELLED and COMPLETED are terminal — no further transitions allowed.

                    Additional guards:
                    - Cannot cancel if active bookings exist.
                    - Cannot complete before the showtime end-time.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Showtime status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Showtime not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - cannot cancel showtime with active bookings")
    })
    public ApiResponse<ShowtimeResponse> updateStatus(
            @PathVariable Long showtimeId,
            @RequestParam ShowtimeStatus status,
            @RequestParam(required = false) String reason
    ) {
        if (status == ShowtimeStatus.CANCELLED && reason != null && !reason.isBlank()) {
            return ApiResponse.success(
                    showtimeService.cancelShowtime(showtimeId, reason),
                    "Showtime cancelled and refund process initiated"
            );
        }
        return ApiResponse.success(showtimeService.updateStatus(showtimeId, status), "Showtime status updated successfully");
    }

    @PostMapping("/{showtimeId}/cancel")
    @Operation(
            summary = "Cancel showtime and trigger automatic refunds (Admin)",
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

    @PostMapping("/{showtimeId}/cancel-and-refund")
    @Operation(
            summary = "Cancel showtime and start system-triggered bulk refund (Admin)",
            description = "Cancels the showtime and processes all PAID bookings for refund."
    )
    public ApiResponse<BulkRefundResponse> cancelShowtimeAndRefund(
            @PathVariable Long showtimeId,
            @Valid @RequestBody(required = false) CancelShowtimeRequest request
    ) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.success(
                showtimeService.cancelShowtimeAndRefund(showtimeId, reason),
                "Showtime cancelled and bulk refund started"
        );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @DeleteMapping("/{showtimeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete showtime (Admin)",
            description = """
                    Permanently delete a showtime from the database.
                    - Rejected with **409 Conflict** if the showtime has any active bookings
                      (HOLDING / PENDING_PAYMENT / PAID / REFUND_REQUESTED).
                    - Cancel all related bookings first before deleting.
                    - For a soft \"hide\" approach prefer `PATCH /{id}/status` with status=CANCELLED.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Showtime deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Showtime not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - showtime has active bookings")
    })
    public void deleteShowtime(@PathVariable Long showtimeId) {
        showtimeService.delete(showtimeId);
    }
}
