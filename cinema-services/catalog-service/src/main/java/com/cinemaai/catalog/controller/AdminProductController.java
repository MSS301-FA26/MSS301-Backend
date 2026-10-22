package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.request.product.ProductCategoryRequest;
import com.cinemaai.catalog.dto.request.product.ProductBundleRequest;
import com.cinemaai.catalog.dto.request.product.ProductRequest;
import com.cinemaai.catalog.dto.response.product.ProductCategoryResponse;
import com.cinemaai.catalog.dto.response.product.ProductBundleResponse;
import com.cinemaai.catalog.dto.response.product.ProductResponse;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.service.ProductCategoryService;
import com.cinemaai.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/foods")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Food & Combo", description = "Admin food management endpoints - requires ADMIN role")
public class AdminProductController {

    private final ProductService productService;
    private final ProductCategoryService productCategoryService;

    // ── Categories Endpoints ────────────────────────────────────────────────
    @GetMapping("/categories")
    @Operation(summary = "Get all food categories (Admin)", description = "Get all food categories (Admin only)")
    public ApiResponse<List<ProductCategoryResponse>> getCategories() {
        return ApiResponse.success(productCategoryService.getAllCategories());
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create food category (Admin)", description = "Create a new food category (Admin only)")
    public ApiResponse<ProductCategoryResponse> createCategory(@Valid @RequestBody ProductCategoryRequest request) {
        return ApiResponse.success(productCategoryService.createCategory(request), "Category created successfully");
    }

    @PutMapping("/categories/{categoryId}")
    @Operation(summary = "Update food category (Admin)", description = "Update an existing food category (Admin only)")
    public ApiResponse<ProductCategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody ProductCategoryRequest request
    ) {
        return ApiResponse.success(productCategoryService.updateCategory(categoryId, request), "Category updated successfully");
    }

    @DeleteMapping("/categories/{categoryId}")
    @Operation(summary = "Delete food category (Admin)", description = "Soft delete / deactivate a food category (Admin only)")
    public ApiResponse<ProductCategoryResponse> deleteCategory(@PathVariable Long categoryId) {
        return ApiResponse.success(productCategoryService.deleteCategory(categoryId), "Category deleted successfully");
    }

    @PostMapping("/categories/{categoryId}/restore")
    @Operation(summary = "Restore food category (Admin)", description = "Restore an inactive food category (Admin only)")
    public ApiResponse<ProductCategoryResponse> restoreCategory(@PathVariable Long categoryId) {
        return ApiResponse.success(productCategoryService.restoreCategory(categoryId), "Category restored successfully");
    }

    // ── Food Items Endpoints ────────────────────────────────────────────────
    @GetMapping("/items")
    @Operation(summary = "Get all food items (Admin)", description = "Get all food items (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Food items retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    public ApiResponse<PageResponse<ProductResponse>> getItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(productService.getAllItems(page, size));
    }

    @GetMapping("/combos")
    @Operation(summary = "Get all food combos (Admin)", description = "Get all food combos (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Food combos retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    public ApiResponse<PageResponse<ProductBundleResponse>> getCombos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(productService.getAllCombos(page, size));
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
    public ApiResponse<ProductResponse> createItem(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(productService.createItem(request), "Food item created successfully");
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
    public ApiResponse<ProductBundleResponse> createCombo(@Valid @RequestBody ProductBundleRequest request) {
        return ApiResponse.success(productService.createCombo(request), "Food combo created successfully");
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
    public ApiResponse<ProductResponse> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody ProductRequest request
    ) {
        return ApiResponse.success(productService.updateItem(itemId, request), "Food item updated successfully");
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
    public ApiResponse<ProductBundleResponse> updateCombo(
            @PathVariable Long comboId,
            @Valid @RequestBody ProductBundleRequest request
    ) {
        return ApiResponse.success(productService.updateCombo(comboId, request), "Food combo updated successfully");
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<ProductResponse> deleteItem(@PathVariable Long itemId) {
        return ApiResponse.success(productService.deleteItem(itemId), "Food item deleted successfully");
    }

    @DeleteMapping("/combos/{comboId}")
    public ApiResponse<ProductBundleResponse> deleteCombo(@PathVariable Long comboId) {
        return ApiResponse.success(productService.deleteCombo(comboId), "Food combo deleted successfully");
    }
}
