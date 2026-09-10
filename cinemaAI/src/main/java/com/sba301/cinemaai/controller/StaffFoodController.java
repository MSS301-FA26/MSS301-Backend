package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.food.FoodComboResponse;
import com.sba301.cinemaai.dto.response.food.FoodItemResponse;
import com.sba301.cinemaai.enums.FoodItemStatus;
import com.sba301.cinemaai.service.FoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/foods")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Staff - Food & Combo", description = "Staff food status endpoints - requires ADMIN or STAFF role")
public class StaffFoodController {

    private final FoodService foodService;

    @GetMapping("/items")
    @Operation(summary = "Get food items for staff", description = "Get all food items so staff can monitor availability")
    public ApiResponse<PageResponse<FoodItemResponse>> getItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(foodService.getAllItems(page, size));
    }

    @GetMapping("/combos")
    @Operation(summary = "Get food combos for staff", description = "Get all food combos so staff can monitor availability")
    public ApiResponse<PageResponse<FoodComboResponse>> getCombos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(foodService.getAllCombos(page, size));
    }

    @PatchMapping("/items/{itemId}/status")
    @Operation(summary = "Update food item status for staff", description = "Quickly mark a food item as active, low stock, or out of stock")
    public ApiResponse<FoodItemResponse> updateItemStatus(
            @PathVariable Long itemId,
            @RequestParam FoodItemStatus status
    ) {
        return ApiResponse.success(foodService.updateItemStatus(itemId, status), "Food item status updated successfully");
    }

    @PatchMapping("/combos/{comboId}/status")
    @Operation(summary = "Update food combo status for staff", description = "Quickly mark a food combo as active, low stock, or out of stock")
    public ApiResponse<FoodComboResponse> updateComboStatus(
            @PathVariable Long comboId,
            @RequestParam FoodItemStatus status
    ) {
        return ApiResponse.success(foodService.updateComboStatus(comboId, status), "Food combo status updated successfully");
    }
}
