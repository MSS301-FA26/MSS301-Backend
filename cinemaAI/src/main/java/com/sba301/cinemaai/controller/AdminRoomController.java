package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.cinema.RoomRequest;
import com.sba301.cinemaai.dto.response.cinema.RoomResponse;
import com.sba301.cinemaai.dto.request.cinema.SeatLayoutRequest;
import com.sba301.cinemaai.dto.response.cinema.SeatResponse;
import com.sba301.cinemaai.dto.request.cinema.SeatUpdateRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.enums.RoomStatus;
import com.sba301.cinemaai.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/admin/rooms")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Rooms", description = "Admin room management endpoints - requires ADMIN role")
public class AdminRoomController {

    private final RoomService roomService;

    @GetMapping
    @Operation(summary = "Get configured cinema rooms (Admin)", description = "Get all rooms in the single configured cinema (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rooms retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cinema not found")
    })
    public ApiResponse<List<RoomResponse>> getRooms() {
        return ApiResponse.success(roomService.getRooms());
    }

    @GetMapping("/{roomId}")
    @Operation(summary = "Get room by ID (Admin)", description = "Get a specific room by ID (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ApiResponse<RoomResponse> getRoom(@PathVariable Long roomId) {
        return ApiResponse.success(roomService.getRoom(roomId));
    }

    @GetMapping("/{roomId}/seats")
    @Operation(summary = "Get room seats (Admin)", description = "Get all seats in a room (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seats retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ApiResponse<List<SeatResponse>> getSeats(@PathVariable Long roomId) {
        return ApiResponse.success(roomService.getSeats(roomId));
    }

    @GetMapping("/seats/{seatId}")
    public ApiResponse<SeatResponse> getSeat(@PathVariable Long seatId) {
        return ApiResponse.success(roomService.getSeat(seatId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create new room (Admin)",
            description = "Create a room in the configured cinema."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Room created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Room already exists")
    })
    public ApiResponse<RoomResponse> createRoom(@Valid @RequestBody RoomRequest request) {
        return ApiResponse.success(roomService.create(request), "Room created successfully");
    }

    @PutMapping("/{roomId}")
    @Operation(
            summary = "Update room (Admin)",
            description = "Update room information. The room remains in its current cinema and no cinema ID is required."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Room already exists")
    })
    public ApiResponse<RoomResponse> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody RoomRequest request
    ) {
        return ApiResponse.success(roomService.update(roomId, request), "Room updated successfully");
    }

    @PatchMapping("/{roomId}/status")
    @Operation(summary = "Update room status (Admin)", description = "Update the status of a room (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ApiResponse<RoomResponse> updateStatus(
            @PathVariable Long roomId,
            @RequestParam RoomStatus status
    ) {
        return ApiResponse.success(roomService.updateStatus(roomId, status), "Room status updated successfully");
    }

    @PostMapping("/{roomId}/seats/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create room seat layout (Admin)", description = "Create the initial seat layout. Returns 409 if the room already has seats.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Seats created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Room already has seats")
    })
    public ApiResponse<List<SeatResponse>> createSeats(
            @PathVariable Long roomId,
            @Valid @RequestBody SeatLayoutRequest request
    ) {
        return ApiResponse.success(roomService.createSeats(roomId, request), "Seats created successfully");
    }

    @PutMapping("/{roomId}/seats")
    @Operation(summary = "Replace room seat layout (Admin)", description = "Replace the complete seat layout of a room that already has seats.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seat layout replaced successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Room has no seats to replace")
    })
    public ApiResponse<List<SeatResponse>> replaceSeats(
            @PathVariable Long roomId,
            @Valid @RequestBody SeatLayoutRequest request
    ) {
        return ApiResponse.success(roomService.replaceSeats(roomId, request), "Seat layout replaced successfully");
    }

    @PutMapping("/seats/{seatId}")
    public ApiResponse<SeatResponse> updateSeat(
            @PathVariable Long seatId,
            @Valid @RequestBody SeatUpdateRequest request
    ) {
        return ApiResponse.success(roomService.updateSeat(seatId, request), "Seat updated successfully");
    }

    @DeleteMapping("/seats/{seatId}")
    public ApiResponse<SeatResponse> deleteSeat(@PathVariable Long seatId) {
        return ApiResponse.success(roomService.deleteSeat(seatId), "Seat deleted successfully");
    }
}
