package com.cinemaai.catalog.mapper;

import com.cinemaai.catalog.dto.response.food.FoodComboResponse;
import com.cinemaai.catalog.dto.response.food.FoodItemResponse;
import com.cinemaai.catalog.entity.FoodCategory;
import com.cinemaai.catalog.entity.FoodCombo;
import com.cinemaai.catalog.entity.FoodItem;
import com.cinemaai.catalog.repository.FoodCategoryRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class FoodMapper {

    @Autowired
    protected FoodCategoryRepository foodCategoryRepository;

    @Mapping(target = "categoryName", source = "categoryId", qualifiedByName = "lookupCategoryName")
    public abstract FoodItemResponse toFoodItemResponse(FoodItem foodItem);

    public abstract FoodComboResponse toFoodComboResponse(FoodCombo foodCombo);

    @org.mapstruct.Named("lookupCategoryName")
    protected String lookupCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return foodCategoryRepository.findById(categoryId)
                .map(FoodCategory::getName)
                .orElse(null);
    }
}
