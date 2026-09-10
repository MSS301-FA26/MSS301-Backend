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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/food-orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Food Order", description = "Standalone and booking-linked food orders")
public class StandaloneFoodOrderController {

    private final FoodOrderService foodOrderService;

    @PostMapping
    @Operation(summary = "Create a standalone food order",
            description = "Creates a counter-pickup food order without requiring a movie booking")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FoodOrderRequest request
    ) {
        FoodOrderResponse response = foodOrderService.createStandalone(user.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Standalone food order created"));
    }

    @GetMapping("/my")
    @Operation(summary = "List my food orders",
            description = "Lists concession orders separately from movie tickets")
    public ResponseEntity<ApiResponse<java.util.List<FoodOrderResponse>>> listMine(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(ApiResponse.success(foodOrderService.listMine(user.getUsername())));
    }

    @DeleteMapping("/{foodOrderId}")
    @Operation(summary = "Cancel a pending food order")
    public ResponseEntity<ApiResponse<FoodOrderResponse>> cancel(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long foodOrderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                foodOrderService.cancel(user.getUsername(), foodOrderId),
                "Food order cancelled"
        ));
    }
}
