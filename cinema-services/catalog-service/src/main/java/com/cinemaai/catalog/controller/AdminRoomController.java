package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.request.cinema.RoomLayoutConfigRequest;
import com.cinemaai.catalog.dto.request.cinema.RoomPricingRequest;
import com.cinemaai.catalog.dto.request.cinema.RoomRequest;
import com.cinemaai.catalog.dto.response.cinema.RoomLayoutConfigResponse;
import com.cinemaai.catalog.dto.response.cinema.RoomPricingResponse;
import com.cinemaai.catalog.dto.response.cinema.RoomResponse;
import com.cinemaai.catalog.dto.request.cinema.SeatLayoutRequest;
import com.cinemaai.catalog.dto.response.cinema.SeatResponse;
import com.cinemaai.catalog.dto.request.cinema.SeatUpdateRequest;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.entity.Room;
import com.cinemaai.catalog.entity.Seat;
import com.cinemaai.catalog.enums.RoomStatus;
import com.cinemaai.catalog.exception.BadRequestException;
import com.cinemaai.catalog.exception.ForbiddenException;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.mapper.CinemaMapper;
import com.cinemaai.catalog.repository.SeatRepository;
import com.cinemaai.catalog.security.AuthenticatedUser;
import com.cinemaai.catalog.security.CinemaSecurityService;
import com.cinemaai.catalog.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequestMapping({"/api/v1/admin/rooms", "/api/v1/admin/rooms/"})
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Rooms", description = "Admin room management endpoints - requires ADMIN or MANAGER role")
public class AdminRoomController {

    private final RoomService roomService;
    private final CinemaSecurityService cinemaSecurityService;
    private final SeatRepository seatRepository;
    private final CinemaMapper cinemaMapper;

    @GetMapping({"", "/"})
    @Operation(summary = "Get cinema rooms (Admin / Manager)", description = "Get rooms optionally filtered by cinema ID. Manager is scoped to their assigned cinema.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rooms retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Access denied to other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cinema not found")
    })
    public ApiResponse<List<RoomResponse>> getRooms(
            @RequestParam(required = false) Long cinemaId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long enforcedCinemaId = cinemaSecurityService.resolveEnforcedCinemaId(user, cinemaId);
        if (enforcedCinemaId != null) {
            return ApiResponse.success(roomService.getRoomsByCinema(enforcedCinemaId));
        }
        return ApiResponse.success(roomService.getRooms());
    }

    @GetMapping("/{roomId}")
    @Operation(summary = "Get room by ID (Admin / Manager)", description = "Get a specific room by ID. Manager can only access rooms of their assigned cinema.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Room belongs to other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ApiResponse<RoomResponse> getRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Room room = roomService.findById(roomId);
        cinemaSecurityService.validateCinemaAccess(user, room.getCinema().getId());
        return ApiResponse.success(cinemaMapper.toRoomResponse(room));
    }

    @GetMapping("/{roomId}/seats")
    @Operation(summary = "Get room seats (Admin / Manager)", description = "Get all seats in a room. Manager can only access rooms of their assigned cinema.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seats retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Room belongs to other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ApiResponse<List<SeatResponse>> getSeats(
            @PathVariable Long roomId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Room room = roomService.findById(roomId);
        cinemaSecurityService.validateCinemaAccess(user, room.getCinema().getId());
        return ApiResponse.success(roomService.getSeats(roomId));
    }

    @GetMapping("/seats/{seatId}")
    public ApiResponse<SeatResponse> getSeat(
            @PathVariable Long seatId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException("Seat not found"));
        cinemaSecurityService.validateCinemaAccess(user, seat.getRoom().getCinema().getId());
        return ApiResponse.success(cinemaMapper.toSeatResponse(seat));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create new room (Admin / Manager)",
            description = "Create a room in the cinema. Manager creates for their assigned cinema."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Room created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not allowed for other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Room already exists")
    })
    public ApiResponse<RoomResponse> createRoom(
            @Valid @RequestBody RoomRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Long targetCinemaId = request.cinemaId();
        if (user != null && user.isManager() && !user.isAdmin()) {
            Long userCinemaId = user.cinemaId();
            if (userCinemaId == null) {
                throw new ForbiddenException("Tài khoản Quản lý chưa được phân công cụm rạp cụ thể.");
            }
            if (targetCinemaId != null && !userCinemaId.equals(targetCinemaId)) {
                throw new ForbiddenException("Quản lý không có quyền tạo phòng chiếu cho rạp khác.");
            }
            targetCinemaId = userCinemaId;
        }
        if (targetCinemaId == null) {
            throw new BadRequestException("Cần chỉ định cụm rạp (cinemaId) cho phòng chiếu.");
        }
        request = new RoomRequest(
                targetCinemaId,
                request.name(),
                request.roomType(),
                request.rowCount(),
                request.columnCount(),
                request.status(),
                request.standardPrice(),
                request.vipPrice(),
                request.couplePrice(),
                request.aislePosition()
        );
        return ApiResponse.success(roomService.create(request), "Room created successfully");
    }

    @PutMapping("/{roomId}")
    @Operation(
            summary = "Update room (Admin / Manager)",
            description = "Update room information. Manager can only update rooms in their assigned cinema."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not allowed for other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Room already exists")
    })
    public ApiResponse<RoomResponse> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody RoomRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Room room = roomService.findById(roomId);
        cinemaSecurityService.validateCinemaAccess(user, room.getCinema().getId());
        Long targetCinemaId = request.cinemaId() != null ? request.cinemaId() : room.getCinema().getId();
        if (user != null && user.isManager() && !user.isAdmin()) {
            Long userCinemaId = user.cinemaId();
            if (request.cinemaId() != null && !userCinemaId.equals(request.cinemaId())) {
                throw new ForbiddenException("Quản lý không có quyền chuyển phòng chiếu sang rạp khác.");
            }
            targetCinemaId = userCinemaId;
        }
        request = new RoomRequest(
                targetCinemaId,
                request.name(),
                request.roomType(),
                request.rowCount(),
                request.columnCount(),
                request.status(),
                request.standardPrice(),
                request.vipPrice(),
                request.couplePrice(),
                request.aislePosition()
        );
        return ApiResponse.success(roomService.update(roomId, request), "Room updated successfully");
    }

    @PatchMapping("/{roomId}/status")
    @Operation(summary = "Update room status (Admin / Manager)", description = "Update the status of a room")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not allowed for other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ApiResponse<RoomResponse> updateStatus(
            @PathVariable Long roomId,
            @RequestParam RoomStatus status,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Room room = roomService.findById(roomId);
        cinemaSecurityService.validateCinemaAccess(user, room.getCinema().getId());
        return ApiResponse.success(roomService.updateStatus(roomId, status), "Room status updated successfully");
    }

    @PostMapping("/{roomId}/seats/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create room seat layout (Admin / Manager)", description = "Create the initial seat layout. Returns 409 if the room already has seats.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Seats created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not allowed for other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Room already has seats")
    })
    public ApiResponse<List<SeatResponse>> createSeats(
            @PathVariable Long roomId,
            @Valid @RequestBody SeatLayoutRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Room room = roomService.findById(roomId);
        cinemaSecurityService.validateCinemaAccess(user, room.getCinema().getId());
        return ApiResponse.success(roomService.createSeats(roomId, request), "Seats created successfully");
    }

    @PutMapping("/{roomId}/seats")
    @Operation(summary = "Replace room seat layout (Admin / Manager)", description = "Replace the complete seat layout of a room that already has seats.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seat layout replaced successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not allowed for other cinema"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Room has no seats to replace")
    })
    public ApiResponse<List<SeatResponse>> replaceSeats(
            @PathVariable Long roomId,
            @Valid @RequestBody SeatLayoutRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Room room = roomService.findById(roomId);
        cinemaSecurityService.validateCinemaAccess(user, room.getCinema().getId());
        return ApiResponse.success(roomService.replaceSeats(roomId, request), "Seat layout replaced successfully");
    }

    @PutMapping("/seats/{seatId}")
    public ApiResponse<SeatResponse> updateSeat(
            @PathVariable Long seatId,
            @Valid @RequestBody SeatUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException("Seat not found"));
        cinemaSecurityService.validateCinemaAccess(user, seat.getRoom().getCinema().getId());
        return ApiResponse.success(roomService.updateSeat(seatId, request), "Seat updated successfully");
    }

    @DeleteMapping("/seats/{seatId}")
    public ApiResponse<SeatResponse> deleteSeat(
            @PathVariable Long seatId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException("Seat not found"));
        cinemaSecurityService.validateCinemaAccess(user, seat.getRoom().getCinema().getId());
        return ApiResponse.success(roomService.deleteSeat(seatId), "Seat deleted successfully");
    }

    @GetMapping("/{roomId}/pricing")
    @Operation(summary = "Get room seat pricing (Admin / Manager)", description = "Get standard, VIP, and couple seat prices for a room.")
    public ApiResponse<RoomPricingResponse> getPricing(
            @PathVariable Long roomId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Room room = roomService.findById(roomId);
        cinemaSecurityService.validateCinemaAccess(user, room.getCinema().getId());
        return ApiResponse.success(roomService.getRoomPricing(roomId));
    }

    @PutMapping("/{roomId}/pricing")
    @Operation(summary = "Update room seat pricing (Admin / Manager)", description = "Update standard, VIP, and couple seat prices for a room.")
    public ApiResponse<RoomPricingResponse> updatePricing(
            @PathVariable Long roomId,
            @Valid @RequestBody RoomPricingRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Room room = roomService.findById(roomId);
        cinemaSecurityService.validateCinemaAccess(user, room.getCinema().getId());
        return ApiResponse.success(roomService.updateRoomPricing(roomId, request), "Room pricing updated successfully");
    }

    @GetMapping("/{roomId}/layout-config")
    @Operation(summary = "Get room layout configuration (Admin / Manager)", description = "Get row count, column count, and aisle position for a room.")
    public ApiResponse<RoomLayoutConfigResponse> getLayoutConfig(
            @PathVariable Long roomId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Room room = roomService.findById(roomId);
        cinemaSecurityService.validateCinemaAccess(user, room.getCinema().getId());
        return ApiResponse.success(roomService.getRoomLayoutConfig(roomId));
    }

    @PutMapping("/{roomId}/layout-config")
    @Operation(summary = "Update room layout configuration (Admin / Manager)", description = "Update row count, column count, and aisle position for a room.")
    public ApiResponse<RoomLayoutConfigResponse> updateLayoutConfig(
            @PathVariable Long roomId,
            @Valid @RequestBody RoomLayoutConfigRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Room room = roomService.findById(roomId);
        cinemaSecurityService.validateCinemaAccess(user, room.getCinema().getId());
        return ApiResponse.success(roomService.updateRoomLayoutConfig(roomId, request), "Cập nhật cấu hình hàng ghế và lối đi thành công");
    }
}
