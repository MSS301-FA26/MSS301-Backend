package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.food.FoodComboResponse;
import com.cinemaai.catalog.dto.response.food.FoodItemResponse;
import com.cinemaai.catalog.enums.FoodItemStatus;
import com.cinemaai.catalog.service.FoodService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/staff/foods")
@Tag(name = "Staff Food Management")
@RequiredArgsConstructor
public class StaffFoodController {

    private final FoodService foodService;

    @GetMapping("/items")
    public ApiResponse<PageResponse<FoodItemResponse>> getItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return ApiResponse.success(foodService.getAllItems(page, size));
    }

    @GetMapping("/combos")
    public ApiResponse<PageResponse<FoodComboResponse>> getCombos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return ApiResponse.success(foodService.getAllCombos(page, size));
    }

    @PatchMapping("/items/{itemId}/status")
    public ApiResponse<FoodItemResponse> updateItemStatus(
            @PathVariable Long itemId,
            @RequestParam(required = false) String status,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String targetStatus = status;
        if ((targetStatus == null || targetStatus.isBlank()) && body != null) {
            targetStatus = body.get("status");
        }
        FoodItemStatus parsed = parseStatus(targetStatus);
        return ApiResponse.success(foodService.updateItemStatus(itemId, parsed), "Cập nhật trạng thái món thành công");
    }

    @PatchMapping("/combos/{comboId}/status")
    public ApiResponse<FoodComboResponse> updateComboStatus(
            @PathVariable Long comboId,
            @RequestParam(required = false) String status,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String targetStatus = status;
        if ((targetStatus == null || targetStatus.isBlank()) && body != null) {
            targetStatus = body.get("status");
        }
        FoodItemStatus parsed = parseStatus(targetStatus);
        return ApiResponse.success(foodService.updateComboStatus(comboId, parsed), "Cập nhật trạng thái combo thành công");
    }

    private FoodItemStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return FoodItemStatus.ACTIVE;
        try {
            return FoodItemStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            if ("INACTIVE".equalsIgnoreCase(status) || "HET".equalsIgnoreCase(status)) {
                return FoodItemStatus.OUT_OF_STOCK;
            }
            return FoodItemStatus.ACTIVE;
        }
    }
}
