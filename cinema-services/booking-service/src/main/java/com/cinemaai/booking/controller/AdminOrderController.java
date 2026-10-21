package com.cinemaai.booking.controller;

import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.dto.response.FoodOrderResponse;
import com.cinemaai.booking.dto.response.PageResponse;
import com.cinemaai.booking.service.FoodOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/food-orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Food Orders", description = "Read-only global FoodOrder visibility for administrators")
public class AdminOrderController {

    private final FoodOrderService foodOrderService;

    @GetMapping
    @Operation(summary = "List all food orders")
    public ApiResponse<PageResponse<FoodOrderResponse>> listFoodOrders(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(foodOrderService.listAll(pageable));
    }
}
