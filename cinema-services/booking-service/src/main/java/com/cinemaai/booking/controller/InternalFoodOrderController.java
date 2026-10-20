package com.cinemaai.booking.controller;

import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.dto.response.FoodOrderResponse;
import com.cinemaai.booking.service.FoodOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/food-orders")
@RequiredArgsConstructor
@Tag(name = "Internal Food Order API", description = "Internal communication between microservices")
public class InternalFoodOrderController {

    private final FoodOrderService foodOrderService;

    @Operation(summary = "Lấy chi tiết đơn bắp nước (Internal)")
    @GetMapping("/{foodOrderId}")
    public ApiResponse<FoodOrderResponse> getFoodOrderInternal(@PathVariable Long foodOrderId) {
        return ApiResponse.success(foodOrderService.getById(foodOrderId));
    }

    @Operation(summary = "Đánh dấu đơn bắp nước đã thanh toán (Internal)")
    @PostMapping("/{foodOrderId}/mark-paid")
    public ApiResponse<FoodOrderResponse> markFoodOrderPaidInternal(
            @PathVariable Long foodOrderId,
            @RequestParam(required = false) String transactionId
    ) {
        return ApiResponse.success(foodOrderService.markPaid(foodOrderId, transactionId));
    }
}