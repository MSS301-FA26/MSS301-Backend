package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.food.FoodCategoryRequest;
import com.sba301.cinemaai.dto.response.food.FoodCategoryResponse;
import java.util.List;

public interface FoodCategoryService {

    List<FoodCategoryResponse> getAllCategories();

    FoodCategoryResponse getCategoryById(Long id);

    FoodCategoryResponse createCategory(FoodCategoryRequest request);

    FoodCategoryResponse updateCategory(Long id, FoodCategoryRequest request);

    void deleteCategory(Long id);

    FoodCategoryResponse restoreCategory(Long id);
}
