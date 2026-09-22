package com.cinemaai.catalog.mapper;

import com.cinemaai.catalog.dto.response.food.FoodComboResponse;
import com.cinemaai.catalog.dto.response.food.FoodItemResponse;
import com.cinemaai.catalog.entity.FoodCombo;
import com.cinemaai.catalog.entity.FoodItem;
import org.springframework.stereotype.Component;

@Component
public class FoodMapper {

    public FoodItemResponse toFoodItemResponse(FoodItem foodItem) {
        return new FoodItemResponse(
                foodItem.getId(),
                foodItem.getName(),
                foodItem.getDescription(),
                foodItem.getPrice(),
                foodItem.getImageUrl(),
                foodItem.getStatus()
        );
    }

    public FoodComboResponse toFoodComboResponse(FoodCombo foodCombo) {
        return new FoodComboResponse(
                foodCombo.getId(),
                foodCombo.getName(),
                foodCombo.getDescription(),
                foodCombo.getPrice(),
                foodCombo.getImageUrl(),
                foodCombo.getStatus()
        );
    }
}
