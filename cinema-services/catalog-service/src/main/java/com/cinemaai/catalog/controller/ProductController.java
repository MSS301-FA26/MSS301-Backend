package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.response.product.ProductCategoryResponse;
import com.cinemaai.catalog.dto.response.product.ProductBundleResponse;
import com.cinemaai.catalog.dto.response.product.ProductResponse;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.enums.ProductStatus;
import com.cinemaai.catalog.service.ProductCategoryService;
import com.cinemaai.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/foods")
@Tag(name = "Food & Combo")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductCategoryService productCategoryService;

    @GetMapping("/categories")
    public ApiResponse<List<ProductCategoryResponse>> getCategories() {
        return ApiResponse.success(productCategoryService.getActiveCategories());
    }

    @GetMapping("/items")
    public ApiResponse<PageResponse<ProductResponse>> getItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(productService.getActiveItems(page, size));
    }

    @GetMapping("/combos")
    public ApiResponse<PageResponse<ProductBundleResponse>> getCombos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(productService.getActiveCombos(page, size));
    }

    @GetMapping("/all/items")
    public ApiResponse<PageResponse<ProductResponse>> getAllItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return ApiResponse.success(productService.getAllItems(page, size));
    }

    @GetMapping("/all/combos")
    public ApiResponse<PageResponse<ProductBundleResponse>> getAllCombos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return ApiResponse.success(productService.getAllCombos(page, size));
    }

    @PatchMapping("/items/{itemId}/status")
    public ApiResponse<ProductResponse> updateItemStatus(
            @PathVariable Long itemId,
            @RequestParam(required = false) String status,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String targetStatus = status;
        if ((targetStatus == null || targetStatus.isBlank()) && body != null) {
            targetStatus = body.get("status");
        }
        ProductStatus parsed = parseStatus(targetStatus);
        return ApiResponse.success(productService.updateItemStatus(itemId, parsed), "Cập nhật trạng thái món thành công");
    }

    @PatchMapping("/combos/{comboId}/status")
    public ApiResponse<ProductBundleResponse> updateComboStatus(
            @PathVariable Long comboId,
            @RequestParam(required = false) String status,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String targetStatus = status;
        if ((targetStatus == null || targetStatus.isBlank()) && body != null) {
            targetStatus = body.get("status");
        }
        ProductStatus parsed = parseStatus(targetStatus);
        return ApiResponse.success(productService.updateComboStatus(comboId, parsed), "Cập nhật trạng thái combo thành công");
    }

    private ProductStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return ProductStatus.ACTIVE;
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            if ("INACTIVE".equalsIgnoreCase(status) || "HET".equalsIgnoreCase(status)) {
                return ProductStatus.OUT_OF_STOCK;
            }
            return ProductStatus.ACTIVE;
        }
    }
}
