package com.sba301.cinemaai.mapper;

import com.sba301.cinemaai.dto.response.food.ComboItemResponse;
import com.sba301.cinemaai.dto.response.food.FoodComboResponse;
import com.sba301.cinemaai.dto.response.food.FoodItemResponse;
import com.sba301.cinemaai.entity.FoodCombo;
import com.sba301.cinemaai.entity.FoodComboItem;
import com.sba301.cinemaai.entity.FoodItem;
import com.sba301.cinemaai.enums.FoodStockStatus;
import com.sba301.cinemaai.repository.FoodComboItemRepository;
import com.sba301.cinemaai.repository.FoodInventoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FoodMapper {

    private final FoodInventoryRepository foodInventoryRepository;
    private final FoodComboItemRepository foodComboItemRepository;

    public FoodItemResponse toFoodItemResponse(FoodItem foodItem) {
        int totalStock = foodInventoryRepository.getTotalAvailableStock(foodItem.getId());
        FoodStockStatus stockStatus;
        if (!foodItem.isStockTracking()) {
            stockStatus = FoodStockStatus.IN_STOCK;
        } else if (totalStock <= 0) {
            stockStatus = FoodStockStatus.OUT_OF_STOCK;
        } else if (totalStock <= foodItem.getLowStockThreshold()) {
            stockStatus = FoodStockStatus.LOW_STOCK;
        } else {
            stockStatus = FoodStockStatus.IN_STOCK;
        }

        BigDecimal cost = foodItem.getCostPrice() != null ? foodItem.getCostPrice() : BigDecimal.ZERO;
        BigDecimal price = foodItem.getPrice() != null ? foodItem.getPrice() : BigDecimal.ZERO;
        BigDecimal grossProfit = price.subtract(cost);
        Double margin = (price.compareTo(BigDecimal.ZERO) > 0)
                ? grossProfit.divide(price, 4, RoundingMode.HALF_UP).doubleValue() * 100.0
                : 0.0;

        return new FoodItemResponse(
                foodItem.getId(),
                foodItem.getSku(),
                foodItem.getName(),
                foodItem.getDescription(),
                foodItem.getCategory() != null ? foodItem.getCategory().getId() : null,
                foodItem.getCategory() != null ? foodItem.getCategory().getName() : null,
                cost,
                price,
                foodItem.getImageUrl(),
                foodItem.getStatus(),
                foodItem.isStockTracking(),
                foodItem.getLowStockThreshold(),
                totalStock,
                stockStatus,
                foodItem.getDeletedAt(),
                grossProfit,
                margin
        );
    }

    public FoodComboResponse toFoodComboResponse(FoodCombo foodCombo) {
        List<FoodComboItem> comboItems = foodComboItemRepository.findByComboIdWithItems(foodCombo.getId());

        BigDecimal regularSum = BigDecimal.ZERO;
        int minPossibleCombos = Integer.MAX_VALUE;

        List<ComboItemResponse> itemResponses = new java.util.ArrayList<>();
        for (FoodComboItem ci : comboItems) {
            FoodItem fi = ci.getFoodItem();
            BigDecimal unitPrice = fi.getPrice() != null ? fi.getPrice() : BigDecimal.ZERO;
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()));
            regularSum = regularSum.add(subtotal);

            itemResponses.add(new ComboItemResponse(
                    ci.getId(),
                    fi.getId(),
                    fi.getName(),
                    fi.getSku(),
                    unitPrice,
                    ci.getQuantity(),
                    subtotal
            ));

            if (fi.isStockTracking()) {
                int itemStock = foodInventoryRepository.getTotalAvailableStock(fi.getId());
                int possible = itemStock / Math.max(ci.getQuantity(), 1);
                if (possible < minPossibleCombos) {
                    minPossibleCombos = possible;
                }
            }
        }

        int maxCombos = comboItems.isEmpty() ? 0 : (minPossibleCombos == Integer.MAX_VALUE ? 999 : Math.max(0, minPossibleCombos));
        FoodStockStatus stockStatus;
        if (maxCombos <= 0) {
            stockStatus = FoodStockStatus.OUT_OF_STOCK;
        } else if (maxCombos <= 5) {
            stockStatus = FoodStockStatus.LOW_STOCK;
        } else {
            stockStatus = FoodStockStatus.IN_STOCK;
        }

        BigDecimal cost = foodCombo.getCostPrice() != null ? foodCombo.getCostPrice() : BigDecimal.ZERO;
        BigDecimal price = foodCombo.getPrice() != null ? foodCombo.getPrice() : BigDecimal.ZERO;
        BigDecimal savings = regularSum.compareTo(price) > 0 ? regularSum.subtract(price) : BigDecimal.ZERO;
        BigDecimal grossProfit = price.subtract(cost);
        Double margin = (price.compareTo(BigDecimal.ZERO) > 0)
                ? grossProfit.divide(price, 4, RoundingMode.HALF_UP).doubleValue() * 100.0
                : 0.0;

        return new FoodComboResponse(
                foodCombo.getId(),
                foodCombo.getSku(),
                foodCombo.getName(),
                foodCombo.getDescription(),
                foodCombo.getCategory() != null ? foodCombo.getCategory().getId() : null,
                foodCombo.getCategory() != null ? foodCombo.getCategory().getName() : null,
                cost,
                price,
                foodCombo.getImageUrl(),
                foodCombo.getStatus(),
                maxCombos,
                stockStatus,
                foodCombo.getDeletedAt(),
                regularSum,
                savings,
                grossProfit,
                margin,
                itemResponses
        );
    }
}
