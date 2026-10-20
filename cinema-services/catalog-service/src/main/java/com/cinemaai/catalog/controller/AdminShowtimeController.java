package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.request.cinema.BulkShowtimeRequest;
import com.cinemaai.catalog.dto.request.cinema.ShowtimePreviewRequest;
import com.cinemaai.catalog.dto.request.cinema.ShowtimeRequest;
import com.cinemaai.catalog.dto.response.cinema.ShowtimePricePreviewResponse;
import com.cinemaai.catalog.dto.response.PageResponse;
import java.util.List;
import com.cinemaai.catalog.dto.response.cinema.ShowtimeResponse;
import com.cinemaai.catalog.dto.response.cinema.ShowtimeSeatMapResponse;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.entity.Room;
import com.cinemaai.catalog.entity.Showtime;
import com.cinemaai.catalog.enums.ShowtimeStatus;
import com.cinemaai.catalog.mapper.CinemaMapper;
import com.cinemaai.catalog.security.AuthenticatedUser;
import com.cinemaai.catalog.security.CinemaSecurityService;
import com.cinemaai.catalog.service.RoomService;
import com.cinemaai.catalog.service.ShowtimeService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@Tag(name = "Admin - Showtimes", description = "Admin showtime management endpoints - requires ADMIN or MANAGER role")
public class AdminShowtimeController {

    private final ShowtimeService showtimeService;
    private final RoomService roomService;
    private final CinemaSecurityService cinemaSecurityService;
    private final CinemaMapper cinemaMapper;

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @GetMapping("/available-slots")
    @Operation(
            summary = "Suggest free time slots (Admin / Manager)",
            description = "Lists free start/end time slots for a room on a given date. Scoped to manager's assigned cinema."
    )
    public ApiResponse<List<com.cinemaai.catalog.dto.response.cinema.AvailableSlotResponse>> availableSlots(
            @RequestParam Long roomId,
            @RequestParam Long movieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Room room = roomService.findById(roomId);
        cinemaSecurityService.validateCinemaAccess(user, room.getCinema().getId());
        return ApiResponse.success(showtimeService.getAvailableSlots(roomId, movieId, date));
    }

    @GetMapping
    @Operation(
            summary = "Search showtimes with paging (Admin / Manager)",
            description = """
                    Search showtimes with optional filters.
                    Manager is scoped strictly to their assigned cinema.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Showtimes retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Access denied to other cinema")
    })
    public ApiResponse<PageResponse<ShowtimeResponse>> searchShowtimes(
            @Parameter(description = "Filter by movie ID") @RequestParam(required = false) Long movieId,
            @Parameter(description = "Filter by room ID")  @RequestParam(required = false) Long roomId,
            @Parameter(description = "Filter by cinema ID") @RequestParam(required = false) Long cinemaId,
            @Parameter(description = "Filter by showtime status") @RequestParam(required = false) ShowtimeStatus status,
            @Parameter(description = "Filter by date (ISO format: yyyy-MM-dd)")
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Page size (max 100)")   @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long enforcedCinemaId = cinemaSecurityService.resolveEnforcedCinemaId(user, cinemaId);
        if (roomId != null) {
            Room room = roomService.findById(roomId);
            cinemaSecurityService.validateCinemaAccess(user, room.getCinema().getId());
        }
        return ApiResponse.success(showtimeService.searchAdmin(movieId, roomId, enforcedCinemaId, status, date, page, size));
    }

    @GetMapping("/{showtimeId}")
    @Operation(
            summary = "Get showtime by ID (Admin / Manager)",
            description = "Retrieve a single showtime by its ID. Manager can only access showtimes of their assigned cinema."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Showtime retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Access denied to other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Showtime not found")
    })
    public ApiResponse<ShowtimeResponse> getShowtime(
            @PathVariable Long showtimeId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Showtime showtime = showtimeService.findById(showtimeId);
        cinemaSecurityService.validateCinemaAccess(user, showtime.getRoom().getCinema().getId());
        return ApiResponse.success(cinemaMapper.toShowtimeResponse(showtime));
    }

    @GetMapping("/{showtimeId}/seat-map")
    @Operation(
            summary = "Get showtime seat map (Admin / Manager)",
            description = "Returns the seat map for a showtime with real-time booking status."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seat map retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Access denied to other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Showtime not found")
    })
    public ApiResponse<ShowtimeSeatMapResponse> getSeatMap(
            @PathVariable Long showtimeId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Showtime showtime = showtimeService.findById(showtimeId);
        cinemaSecurityService.validateCinemaAccess(user, showtime.getRoom().getCinema().getId());
        return ApiResponse.success(showtimeService.getSeatMap(showtimeId));
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create new showtime (Admin / Manager)",
            description = "Create a new showtime in the manager's assigned cinema or any cinema for Admin."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Showtime created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or business rule violation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not allowed for other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie or room not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - room already has an overlapping showtime")
    })
    public ApiResponse<ShowtimeResponse> createShowtime(
            @Valid @RequestBody ShowtimeRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Room room = roomService.findById(request.roomId());
        cinemaSecurityService.validateCinemaAccess(user, room.getCinema().getId());
        return ApiResponse.success(showtimeService.create(request), "Showtime created successfully");
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Bulk create showtimes for one movie (Admin / Manager)",
            description = "Create multiple showtime slots for the same movie. Manager can only create slots in their assigned cinema."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "All showtimes created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not allowed for other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie or one of the rooms not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - schedule overlap")
    })
    public ApiResponse<List<ShowtimeResponse>> createBulkShowtimes(
            @Valid @RequestBody BulkShowtimeRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        if (request.slots() != null) {
            for (var slot : request.slots()) {
                Room room = roomService.findById(slot.roomId());
                cinemaSecurityService.validateCinemaAccess(user, room.getCinema().getId());
            }
        }
        return ApiResponse.success(showtimeService.createBulk(request),
                request.slots().size() + " showtime(s) created successfully");
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @PutMapping("/{showtimeId}")
    @Operation(
            summary = "Update showtime (Admin / Manager)",
            description = "Update showtime details. Manager can only update showtimes belonging to their assigned cinema."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Showtime updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not allowed for other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Showtime, movie or room not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - schedule overlap or active bookings")
    })
    public ApiResponse<ShowtimeResponse> updateShowtime(
            @PathVariable Long showtimeId,
            @Valid @RequestBody ShowtimeRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Showtime showtime = showtimeService.findById(showtimeId);
        cinemaSecurityService.validateCinemaAccess(user, showtime.getRoom().getCinema().getId());
        Room newRoom = roomService.findById(request.roomId());
        cinemaSecurityService.validateCinemaAccess(user, newRoom.getCinema().getId());
        return ApiResponse.success(showtimeService.update(showtimeId, request), "Showtime updated successfully");
    }

    @PatchMapping("/{showtimeId}/status")
    @Operation(
            summary = "Update showtime status (Admin / Manager)",
            description = "Change the status of a showtime. Manager can only update showtimes belonging to their assigned cinema."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Showtime status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not allowed for other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Showtime not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - cannot cancel showtime with active bookings")
    })
    public ApiResponse<ShowtimeResponse> updateStatus(
            @PathVariable Long showtimeId,
            @RequestParam ShowtimeStatus status,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Showtime showtime = showtimeService.findById(showtimeId);
        cinemaSecurityService.validateCinemaAccess(user, showtime.getRoom().getCinema().getId());
        if (status == ShowtimeStatus.CANCELLED && reason != null && !reason.isBlank()) {
            return ApiResponse.success(
                    showtimeService.cancelShowtime(showtimeId, reason),
                    "Showtime cancelled and refund process initiated"
            );
        }
        return ApiResponse.success(showtimeService.updateStatus(showtimeId, status), "Showtime status updated successfully");
    }

    @PostMapping(value = {"/{showtimeId}/cancel", "/{showtimeId}/cancel-and-refund"})
    @Operation(
            summary = "Cancel showtime and trigger automatic refunds (Admin / Manager)",
            description = "Cancels the showtime and automatically processes refunds. Manager can only cancel showtimes of their assigned cinema."
    )
    public ApiResponse<ShowtimeResponse> cancelShowtime(
            @PathVariable Long showtimeId,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) com.cinemaai.catalog.dto.request.refund.CancelShowtimeRequest requestBody,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        String finalReason = reason;
        if ((finalReason == null || finalReason.isBlank()) && requestBody != null) {
            finalReason = requestBody.reason();
        }
        if (finalReason == null || finalReason.isBlank()) {
            finalReason = "Huy suat chieu do su co ky thuat";
        }
        Showtime showtime = showtimeService.findById(showtimeId);
        cinemaSecurityService.validateCinemaAccess(user, showtime.getRoom().getCinema().getId());
        return ApiResponse.success(
                showtimeService.cancelShowtime(showtimeId, finalReason),
                "Showtime cancelled and refund process initiated"
        );
    }

    @DeleteMapping("/{showtimeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete showtime (Admin / Manager)",
            description = "Permanently delete a showtime from the database. Manager can only delete showtimes of their assigned cinema."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Showtime deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not allowed for other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Showtime not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - showtime has active bookings")
    })
    public void deleteShowtime(
            @PathVariable Long showtimeId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Showtime showtime = showtimeService.findById(showtimeId);
        cinemaSecurityService.validateCinemaAccess(user, showtime.getRoom().getCinema().getId());
        showtimeService.delete(showtimeId);
    }

    // -------------------------------------------------------------------------
    // PRICE PREVIEW (no persistence)
    // -------------------------------------------------------------------------

    @PostMapping("/preview-prices")
    @Operation(
            summary = "Preview ticket prices for draft showtime slots (Admin / Manager)",
            description = "Calculates the full ticket price matrix (seat type x audience type) for a set of draft slots. " +
                    "Does NOT create any showtime records in the database. " +
                    "Manager can only preview slots for rooms in their assigned cinema."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Price matrix calculated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not allowed for other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie or room not found")
    })
    public ApiResponse<List<ShowtimePricePreviewResponse>> previewPrices(
            @Valid @RequestBody ShowtimePreviewRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        if (request.slots() != null) {
            for (var slot : request.slots()) {
                Room room = roomService.findById(slot.roomId());
                cinemaSecurityService.validateCinemaAccess(user, room.getCinema().getId());
            }
        }
        return ApiResponse.success(showtimeService.previewPrices(request), "Price preview calculated");
    }
}

