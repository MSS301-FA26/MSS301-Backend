package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.booking.FoodOrderRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.booking.FoodOrderResponse;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.FoodOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/food-orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Food Order", description = "Order more food & drinks on a paid booking")
public class FoodOrderController {

    private final FoodOrderService foodOrderService;

    @PostMapping
    @Operation(summary = "Order more food on a paid booking",
            description = "Creates a pending food order for a PAID/USED booking while the showtime has not ended")
    public ApiResponse<FoodOrderResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookingId,
            @Valid @RequestBody FoodOrderRequest request
    ) {
        return ApiResponse.success(foodOrderService.create(user.getUsername(), bookingId, request), "Food order created");
    }

    @GetMapping
    @Operation(summary = "List food orders of a booking")
    public ApiResponse<List<FoodOrderResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long bookingId
    ) {
        return ApiResponse.success(foodOrderService.listByBooking(user.getUsername(), bookingId));
    }
}
