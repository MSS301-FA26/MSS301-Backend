package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.food.FoodComboRequest;
import com.sba301.cinemaai.dto.request.food.FoodItemRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.food.FoodComboResponse;
import com.sba301.cinemaai.dto.response.food.FoodItemResponse;
import com.sba301.cinemaai.dto.response.food.FoodPriceHistoryResponse;
import com.sba301.cinemaai.enums.FoodItemStatus;
import com.sba301.cinemaai.service.FoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/foods")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Food & Combo", description = "Admin food management endpoints - requires ADMIN role")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFoodController {

    private final FoodService foodService;

    @GetMapping("/items")
    @Operation(summary = "Get food items with search, filter, sort and pagination")
    public ApiResponse<PageResponse<FoodItemResponse>> getItems(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) FoodItemStatus status,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name_asc") String sort
    ) {
        if (search != null || categoryId != null || status != null || includeDeleted || !"name_asc".equals(sort)) {
            return ApiResponse.success(foodService.searchItems(search, categoryId, status, includeDeleted, page, size, sort));
        }
        return ApiResponse.success(foodService.getAllItems(page, size));
    }

    @GetMapping("/combos")
    @Operation(summary = "Get food combos with search, filter, sort and pagination")
    public ApiResponse<PageResponse<FoodComboResponse>> getCombos(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) FoodItemStatus status,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name_asc") String sort
    ) {
        if (search != null || categoryId != null || status != null || includeDeleted || !"name_asc".equals(sort)) {
            return ApiResponse.success(foodService.searchCombos(search, categoryId, status, includeDeleted, page, size, sort));
        }
        return ApiResponse.success(foodService.getAllCombos(page, size));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create food item", description = "Create a new food item with SKU and pricing")
    public ApiResponse<FoodItemResponse> createItem(@Valid @RequestBody FoodItemRequest request) {
        return ApiResponse.success(foodService.createItem(request), "Tạo món thành công");
    }

    @PostMapping("/combos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create food combo", description = "Create a new combo with recipe items")
    public ApiResponse<FoodComboResponse> createCombo(@Valid @RequestBody FoodComboRequest request) {
        return ApiResponse.success(foodService.createCombo(request), "Tạo combo thành công");
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update food item", description = "Update an existing food item")
    public ApiResponse<FoodItemResponse> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody FoodItemRequest request
    ) {
        return ApiResponse.success(foodService.updateItem(itemId, request), "Cập nhật món thành công");
    }

    @PutMapping("/combos/{comboId}")
    @Operation(summary = "Update food combo", description = "Update an existing food combo")
    public ApiResponse<FoodComboResponse> updateCombo(
            @PathVariable Long comboId,
            @Valid @RequestBody FoodComboRequest request
    ) {
        return ApiResponse.success(foodService.updateCombo(comboId, request), "Cập nhật combo thành công");
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Soft delete food item")
    public ApiResponse<FoodItemResponse> deleteItem(@PathVariable Long itemId) {
        return ApiResponse.success(foodService.deleteItem(itemId), "Đã chuyển món vào thùng rác");
    }

    @DeleteMapping("/combos/{comboId}")
    @Operation(summary = "Soft delete food combo")
    public ApiResponse<FoodComboResponse> deleteCombo(@PathVariable Long comboId) {
        return ApiResponse.success(foodService.deleteCombo(comboId), "Đã chuyển combo vào thùng rác");
    }

    @PostMapping("/items/{itemId}/restore")
    @Operation(summary = "Restore soft deleted food item")
    public ApiResponse<FoodItemResponse> restoreItem(@PathVariable Long itemId) {
        return ApiResponse.success(foodService.restoreItem(itemId), "Khôi phục món thành công");
    }

    @PostMapping("/combos/{comboId}/restore")
    @Operation(summary = "Restore soft deleted food combo")
    public ApiResponse<FoodComboResponse> restoreCombo(@PathVariable Long comboId) {
        return ApiResponse.success(foodService.restoreCombo(comboId), "Khôi phục combo thành công");
    }

    @PostMapping("/items/{itemId}/duplicate")
    @Operation(summary = "Duplicate food item")
    public ApiResponse<FoodItemResponse> duplicateItem(@PathVariable Long itemId) {
        return ApiResponse.success(foodService.duplicateItem(itemId), "Nhân bản món thành công");
    }

    @PostMapping("/combos/{comboId}/duplicate")
    @Operation(summary = "Duplicate food combo")
    public ApiResponse<FoodComboResponse> duplicateCombo(@PathVariable Long comboId) {
        return ApiResponse.success(foodService.duplicateCombo(comboId), "Nhân bản combo thành công");
    }

    @PostMapping("/bulk-status")
    @Operation(summary = "Bulk update status")
    public ApiResponse<Void> bulkUpdateStatus(@RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) payload.get("ids");
        List<Long> ids = rawIds != null ? rawIds.stream().map(Number::longValue).toList() : List.of();
        String kind = (String) payload.getOrDefault("kind", "item");
        String rawStatus = (String) payload.getOrDefault("status", "ACTIVE");
        FoodItemStatus status = FoodItemStatus.valueOf(rawStatus);
        foodService.bulkUpdateStatus(ids, kind, status);
        return ApiResponse.success(null, "Cập nhật trạng thái hàng loạt thành công");
    }

    @PostMapping("/bulk-delete")
    @Operation(summary = "Bulk soft delete")
    public ApiResponse<Void> bulkDelete(@RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) payload.get("ids");
        List<Long> ids = rawIds != null ? rawIds.stream().map(Number::longValue).toList() : List.of();
        String kind = (String) payload.getOrDefault("kind", "item");
        foodService.bulkDelete(ids, kind);
        return ApiResponse.success(null, "Xóa hàng loạt thành công");
    }

    @GetMapping("/items/{itemId}/price-history")
    @Operation(summary = "Get food item price history")
    public ApiResponse<List<FoodPriceHistoryResponse>> getItemPriceHistory(@PathVariable Long itemId) {
        return ApiResponse.success(foodService.getPriceHistory(itemId, "item"));
    }

    @GetMapping("/combos/{comboId}/price-history")
    @Operation(summary = "Get food combo price history")
    public ApiResponse<List<FoodPriceHistoryResponse>> getComboPriceHistory(@PathVariable Long comboId) {
        return ApiResponse.success(foodService.getPriceHistory(comboId, "combo"));
    }
}
