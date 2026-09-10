package com.sba301.cinemaai.mapper;

import com.sba301.cinemaai.dto.response.food.FoodComboResponse;
import com.sba301.cinemaai.dto.response.food.FoodItemResponse;
import com.sba301.cinemaai.entity.FoodCombo;
import com.sba301.cinemaai.entity.FoodItem;
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
