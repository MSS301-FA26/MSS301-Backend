package com.cinemaai.catalog.service;

import com.cinemaai.catalog.dto.request.food.FoodCategoryRequest;
import com.cinemaai.catalog.dto.response.food.FoodCategoryResponse;

import java.util.List;

public interface FoodCategoryService {
    List<FoodCategoryResponse> getActiveCategories();
    List<FoodCategoryResponse> getAllCategories();
    FoodCategoryResponse getCategoryById(Long id);
    FoodCategoryResponse createCategory(FoodCategoryRequest request);
    FoodCategoryResponse updateCategory(Long id, FoodCategoryRequest request);
    FoodCategoryResponse deleteCategory(Long id);
    FoodCategoryResponse restoreCategory(Long id);
}
