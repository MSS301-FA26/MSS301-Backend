package com.cinemaai.catalog.controller;

import com.cinemaai.catalog.dto.request.food.FoodQuoteRequest;
import com.cinemaai.catalog.dto.response.ApiResponse;
import com.cinemaai.catalog.dto.response.food.FoodQuoteResponse;
import com.cinemaai.catalog.entity.FoodCombo;
import com.cinemaai.catalog.entity.FoodItem;
import com.cinemaai.catalog.enums.FoodItemStatus;
import com.cinemaai.catalog.exception.BadRequestException;
import com.cinemaai.catalog.repository.FoodComboRepository;
import com.cinemaai.catalog.repository.FoodItemRepository;
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

@Slf4j
@RestController
@RequestMapping("/internal/v1/foods")
@RequiredArgsConstructor
@Tag(name = "Internal Foods", description = "Internal food quote validation for microservices")
public class InternalFoodController {

    private final FoodItemRepository foodItemRepository;
    private final FoodComboRepository foodComboRepository;

    @Operation(summary = "Quote and validate food items/combos")
    @PostMapping("/quote")
    public ApiResponse<FoodQuoteResponse> quoteFoods(@RequestBody FoodQuoteRequest request) {
        if (request == null || request.foods() == null || request.foods().isEmpty()) {
            return ApiResponse.success(new FoodQuoteResponse(List.of(), BigDecimal.ZERO), "No foods requested");
        }

        List<FoodQuoteResponse.FoodItemSnapshot> resultItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (FoodQuoteRequest.Item item : request.foods()) {
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

            if (resolvedId == null) {
                continue;
            }

            final Long targetId = resolvedId;
            final boolean finalIsCombo = isCombo;
            int qty = item.quantity() != null && item.quantity() > 0 ? item.quantity() : 1;
            String name;
            BigDecimal price;
            FoodItemStatus status;

            if (finalIsCombo) {
                FoodCombo combo = foodComboRepository.findById(targetId)
                        .orElseThrow(() -> new BadRequestException("Food combo does not exist: " + targetId));
                name = combo.getName();
                price = combo.getPrice();
                status = combo.getStatus();
            } else {
                FoodItem foodItem = foodItemRepository.findById(targetId)
                        .orElseThrow(() -> new BadRequestException("Food item does not exist: " + targetId));
                name = foodItem.getName();
                price = foodItem.getPrice();
                status = foodItem.getStatus();
            }

            if (status != FoodItemStatus.ACTIVE && status != FoodItemStatus.LOW_STOCK) {
                throw new BadRequestException("Sản phẩm '" + name + "' hiện không khả dụng (Trạng thái: " + status + ")");
            }

            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(qty));
            totalAmount = totalAmount.add(lineTotal);

            resultItems.add(new FoodQuoteResponse.FoodItemSnapshot(
                    targetId,
                    finalIsCombo,
                    name,
                    price,
                    qty,
                    lineTotal
            ));
        }

        return ApiResponse.success(new FoodQuoteResponse(resultItems, totalAmount), "Food quote calculated successfully");
    }
}