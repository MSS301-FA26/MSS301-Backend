package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.food.FoodComboRequest;
import com.sba301.cinemaai.dto.response.food.FoodComboResponse;
import com.sba301.cinemaai.dto.request.food.FoodItemRequest;
import com.sba301.cinemaai.dto.response.food.FoodItemResponse;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.service.FoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/foods")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Food & Combo", description = "Admin food management endpoints - requires ADMIN role")
public class AdminFoodController {

    private final FoodService foodService;

    @GetMapping("/items")
    @Operation(summary = "Get all food items (Admin)", description = "Get all food items (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Food items retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    public ApiResponse<PageResponse<FoodItemResponse>> getItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(foodService.getAllItems(page, size));
    }

    @GetMapping("/combos")
    @Operation(summary = "Get all food combos (Admin)", description = "Get all food combos (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Food combos retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    public ApiResponse<PageResponse<FoodComboResponse>> getCombos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(foodService.getAllCombos(page, size));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create food item (Admin)", description = "Create a new food item (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Food item created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    public ApiResponse<FoodItemResponse> createItem(@Valid @RequestBody FoodItemRequest request) {
        return ApiResponse.success(foodService.createItem(request), "Food item created successfully");
    }

    @PostMapping("/combos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create food combo (Admin)", description = "Create a new food combo (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Food combo created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    public ApiResponse<FoodComboResponse> createCombo(@Valid @RequestBody FoodComboRequest request) {
        return ApiResponse.success(foodService.createCombo(request), "Food combo created successfully");
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update food item (Admin)", description = "Update an existing food item (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Food item updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Food item not found")
    })
    public ApiResponse<FoodItemResponse> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody FoodItemRequest request
    ) {
        return ApiResponse.success(foodService.updateItem(itemId, request), "Food item updated successfully");
    }

    @PutMapping("/combos/{comboId}")
    @Operation(summary = "Update food combo (Admin)", description = "Update an existing food combo (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Food combo updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Food combo not found")
    })
    public ApiResponse<FoodComboResponse> updateCombo(
            @PathVariable Long comboId,
            @Valid @RequestBody FoodComboRequest request
    ) {
        return ApiResponse.success(foodService.updateCombo(comboId, request), "Food combo updated successfully");
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<FoodItemResponse> deleteItem(@PathVariable Long itemId) {
        return ApiResponse.success(foodService.deleteItem(itemId), "Food item deleted successfully");
    }

    @DeleteMapping("/combos/{comboId}")
    public ApiResponse<FoodComboResponse> deleteCombo(@PathVariable Long comboId) {
        return ApiResponse.success(foodService.deleteCombo(comboId), "Food combo deleted successfully");
    }
}
