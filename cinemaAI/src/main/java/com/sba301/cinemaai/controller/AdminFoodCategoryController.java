package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.food.FoodCategoryRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.food.FoodCategoryResponse;
import com.sba301.cinemaai.service.FoodCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/foods/categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Food Categories", description = "Admin F&B category management endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFoodCategoryController {

    private final FoodCategoryService foodCategoryService;

    @GetMapping
    @Operation(summary = "Get all categories", description = "Retrieves all active food categories sorted by sort order")
    public ApiResponse<List<FoodCategoryResponse>> getAllCategories() {
        return ApiResponse.success(foodCategoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieves details of a single category")
    public ApiResponse<FoodCategoryResponse> getCategoryById(@PathVariable Long id) {
        return ApiResponse.success(foodCategoryService.getCategoryById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create category", description = "Creates a new food category with unique code")
    public ApiResponse<FoodCategoryResponse> createCategory(@Valid @RequestBody FoodCategoryRequest request) {
        return ApiResponse.success(foodCategoryService.createCategory(request), "Category created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category", description = "Updates an existing food category")
    public ApiResponse<FoodCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody FoodCategoryRequest request
    ) {
        return ApiResponse.success(foodCategoryService.updateCategory(id, request), "Category updated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete category", description = "Soft deletes a food category if no products are linked")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        foodCategoryService.deleteCategory(id);
        return ApiResponse.success(null, "Category deleted successfully");
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore category", description = "Restores a previously soft deleted category")
    public ApiResponse<FoodCategoryResponse> restoreCategory(@PathVariable Long id) {
        return ApiResponse.success(foodCategoryService.restoreCategory(id), "Category restored successfully");
    }
}
