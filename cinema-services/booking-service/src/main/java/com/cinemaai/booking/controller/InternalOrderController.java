package com.cinemaai.booking.controller;

import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.dto.response.OrderResponse;
import com.cinemaai.booking.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/food-orders")
@RequiredArgsConstructor
@Tag(name = "Internal Order API", description = "Internal order communication between microservices")
public class InternalOrderController {

    private final OrderService orderService;

    @Operation(summary = "Lấy chi tiết đơn bắp nước (Internal)")
    @GetMapping("/{foodOrderId}")
    public ApiResponse<OrderResponse> getFoodOrderInternal(@PathVariable Long foodOrderId) {
        return ApiResponse.success(orderService.getById(foodOrderId));
    }

    @Operation(summary = "Đánh dấu đơn bắp nước đã thanh toán (Internal)")
    @PostMapping("/{foodOrderId}/mark-paid")
    public ApiResponse<OrderResponse> markFoodOrderPaidInternal(
            @PathVariable Long foodOrderId,
            @RequestParam(required = false) String transactionId
    ) {
        return ApiResponse.success(orderService.markPaid(foodOrderId, transactionId));
    }
}
