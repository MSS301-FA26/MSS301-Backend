package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.food.StockAdjustmentRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.food.FoodInventoryResponse;
import com.sba301.cinemaai.dto.response.food.FoodInventoryTransactionResponse;
import com.sba301.cinemaai.service.FoodInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/foods/inventory")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Food Inventory", description = "Multi-cinema F&B inventory and stock adjustment endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFoodInventoryController {

    private final FoodInventoryService foodInventoryService;

    @GetMapping
    @Operation(summary = "Get inventory by cinema", description = "Retrieves stock of items at a specific cinema")
    public ApiResponse<List<FoodInventoryResponse>> getInventoryByCinema(
            @RequestParam(required = false) Long cinemaId
    ) {
        if (cinemaId != null) {
            return ApiResponse.success(foodInventoryService.getInventoriesByCinema(cinemaId));
        }
        return ApiResponse.success(foodInventoryService.getInventoriesByCinema(1L));
    }

    @GetMapping("/item/{foodItemId}")
    @Operation(summary = "Get inventory of food item across cinemas", description = "Retrieves stock of a food item across all cinemas")
    public ApiResponse<List<FoodInventoryResponse>> getInventoryByItem(@PathVariable Long foodItemId) {
        return ApiResponse.success(foodInventoryService.getInventoriesByFoodItem(foodItemId));
    }

    @PostMapping("/adjust")
    @Operation(summary = "Adjust inventory stock", description = "Adjusts stock with mandatory reason and logs audit transaction")
    public ApiResponse<FoodInventoryResponse> adjustStock(
            @Valid @RequestBody StockAdjustmentRequest request,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : "ADMIN";
        return ApiResponse.success(foodInventoryService.adjustStock(request, username), "Đã điều chỉnh tồn kho thành công");
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get inventory transactions", description = "Retrieves audit log of inventory movements")
    public ApiResponse<PageResponse<FoodInventoryTransactionResponse>> getTransactions(
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) Long foodItemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(foodInventoryService.getTransactions(cinemaId, foodItemId, page, size));
    }
}
