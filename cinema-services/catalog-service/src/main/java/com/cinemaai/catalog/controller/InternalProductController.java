package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.request.product.ProductQuoteRequest;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.product.ProductQuoteResponse;
import com.cinemaai.catalog.entity.ProductBundle;
import com.cinemaai.catalog.entity.Product;
import com.cinemaai.catalog.enums.ProductStatus;
import com.cinemaai.catalog.exception.BadRequestException;
import com.cinemaai.catalog.repository.ProductBundleRepository;
import com.cinemaai.catalog.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/internal/v1/foods")
@RequiredArgsConstructor
@Tag(name = "Internal Foods", description = "Internal food quote validation for microservices")
public class InternalProductController {

    private final ProductRepository productRepository;
    private final ProductBundleRepository productBundleRepository;

    @Operation(summary = "Quote and validate food items/combos")
    @PostMapping("/quote")
    public ApiResponse<ProductQuoteResponse> quoteFoods(@Valid @RequestBody ProductQuoteRequest request) {
        if (request == null || request.foods() == null || request.foods().isEmpty()) {
            return ApiResponse.success(new ProductQuoteResponse(List.of(), BigDecimal.ZERO), "No foods requested");
        }

        List<ProductQuoteResponse.ProductSnapshot> resultItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        java.util.Set<String> productKeys = new java.util.HashSet<>();

        for (ProductQuoteRequest.ProductSelection item : request.foods()) {
            validateFoodIdentifiers(item.productId(), item.isCombo(), item.foodItemId(), item.foodComboId());
            Long resolvedId = item.productId();
            boolean isCombo = Boolean.TRUE.equals(item.isCombo());

            if (resolvedId == null) {
                if (item.foodComboId() != null) {
                    resolvedId = item.foodComboId();
                    isCombo = true;
                } else if (item.foodItemId() != null) {
                    resolvedId = item.foodItemId();
                    isCombo = false;
                }
            }

            if (resolvedId == null) throw new BadRequestException("Food product identifier is required");

            final Long targetId = resolvedId;
            final boolean finalIsCombo = isCombo;
            if (item.quantity() == null || item.quantity() <= 0) throw new BadRequestException("Food quantity must be greater than zero");
            int qty = item.quantity();
            if (!productKeys.add(finalIsCombo + ":" + targetId)) throw new BadRequestException("Duplicate food selection");
            String name;
            BigDecimal price;
            ProductStatus status;

            if (finalIsCombo) {
                ProductBundle combo = productBundleRepository.findById(targetId)
                        .orElseThrow(() -> new BadRequestException("Food combo does not exist: " + targetId));
                name = combo.getName();
                price = combo.getPrice();
                status = combo.getStatus();
            } else {
                Product foodItem = productRepository.findById(targetId)
                        .orElseThrow(() -> new BadRequestException("Food item does not exist: " + targetId));
                name = foodItem.getName();
                price = foodItem.getPrice();
                status = foodItem.getStatus();
            }

            if (status != ProductStatus.ACTIVE && status != ProductStatus.LOW_STOCK) {
                throw new BadRequestException("Sản phẩm '" + name + "' hiện không khả dụng (Trạng thái: " + status + ")");
            }

            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(qty));
            totalAmount = totalAmount.add(lineTotal);

            resultItems.add(new ProductQuoteResponse.ProductSnapshot(
                    targetId,
                    finalIsCombo,
                    name,
                    price,
                    qty,
                    lineTotal
            ));
        }

        return ApiResponse.success(new ProductQuoteResponse(resultItems, totalAmount), "Food quote calculated successfully");
    }

    private void validateFoodIdentifiers(Long productId, Boolean isCombo, Long itemId, Long comboId) {
        if (itemId != null && comboId != null) throw new BadRequestException("Food item and combo identifiers conflict");
        if (productId == null && itemId == null && comboId == null) throw new BadRequestException("Food product identifier is required");
        if (productId != null && itemId != null && (!productId.equals(itemId) || Boolean.TRUE.equals(isCombo)))
            throw new BadRequestException("Food product identifiers conflict");
        if (productId != null && comboId != null && (!productId.equals(comboId) || !Boolean.TRUE.equals(isCombo)))
            throw new BadRequestException("Food product identifiers conflict");
        if (productId != null && itemId == null && comboId == null && isCombo == null)
            throw new BadRequestException("Food product type is required");
    }
}
