package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.booking.BookingResponse;
import com.sba301.cinemaai.dto.request.booking.CheckInRequest;
import com.sba301.cinemaai.dto.request.booking.CheckInSeatsRequest;
import com.sba301.cinemaai.dto.request.booking.FoodOrderRequest;
import com.sba301.cinemaai.dto.request.booking.FoodPickupRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.booking.FoodOrderResponse;
import com.sba301.cinemaai.service.BookingService;
import com.sba301.cinemaai.service.FoodOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/staff/check-in", "/api/v1/admin/check-in"})
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Check-In", description = "Check-in endpoints - requires ADMIN or STAFF role")
public class StaffCheckInController {

    private final BookingService bookingService;
    private final FoodOrderService foodOrderService;

    @PostMapping
    @Operation(summary = "Check in ticket (Staff/Admin)", description = "Check in a ticket using QR code (Admin or Staff only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket checked in successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN or STAFF role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ApiResponse<BookingResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ApiResponse.success(bookingService.checkIn(request.qrCode()), "Ticket checked in successfully");
    }

    @PostMapping("/seats")
    @Operation(summary = "Check in selected seats (Staff/Admin)",
            description = "Check in a subset of seats of a booking by their ticket codes - airline style partial check-in")
    public ApiResponse<BookingResponse> checkInSeats(@Valid @RequestBody CheckInSeatsRequest request) {
        return ApiResponse.success(
                bookingService.checkInSeats(request.bookingCode(), request.ticketCodes()),
                "Seats checked in successfully");
    }

    @PostMapping("/food-orders")
    @Operation(summary = "Sell food at the counter for a booking (Staff/Admin)",
            description = "Creates and immediately confirms (cash) a food order for a PAID/USED booking, looked up by booking code, ticket code or QR")
    public ApiResponse<FoodOrderResponse> createFoodOrder(
            @RequestParam String code,
            @Valid @RequestBody FoodOrderRequest request
    ) {
        return ApiResponse.success(foodOrderService.createStaffOrder(code, request), "Food order confirmed");
    }

    @GetMapping("/food-orders/lookup")
    @Operation(summary = "Lookup a food order for pickup (Staff/Admin)",
            description = "Finds a paid standalone concession order by order code or pickup QR")
    public ApiResponse<FoodOrderResponse> lookupFoodOrder(@RequestParam String code) {
        return ApiResponse.success(foodOrderService.lookupForPickup(code), "Food order found");
    }

    @PostMapping("/food-orders/pickup")
    @Operation(summary = "Confirm food-order pickup (Staff/Admin)",
            description = "Marks a paid concession order as picked up. The operation can only succeed once")
    public ApiResponse<FoodOrderResponse> pickUpFoodOrder(@Valid @RequestBody FoodPickupRequest request) {
        return ApiResponse.success(foodOrderService.markPickedUp(request.code()), "Food order picked up");
    }

    @GetMapping("/lookup")
    @Operation(summary = "Lookup booking for check-in (Staff/Admin)", description = "Find a booking by booking code or QR code")
    public ApiResponse<BookingResponse> lookup(
            @RequestParam(required = false) String bookingCode,
            @RequestParam(required = false) String qrCode
    ) {
        return ApiResponse.success(bookingService.lookupForCheckIn(bookingCode, qrCode), "Booking found");
    }

    @GetMapping("/recent")
    @Operation(summary = "List recent check-in bookings (Staff/Admin)", description = "Get recent paid or checked-in bookings for the staff check-in workspace")
    public ApiResponse<List<BookingResponse>> recentBookings(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(bookingService.getRecentStaffCheckInBookings(limit), "Recent bookings retrieved");
    }

    @GetMapping("/showtimes/{showtimeId}/bookings")
    @Operation(summary = "List bookings by showtime (Staff/Admin)", description = "Get all bookings of a showtime for check-in")
    public ApiResponse<List<BookingResponse>> bookingsByShowtime(@PathVariable Long showtimeId) {
        return ApiResponse.success(bookingService.getStaffBookingsByShowtime(showtimeId), "Bookings retrieved");
    }
}
