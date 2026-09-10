package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.food.FoodComboRequest;
import com.sba301.cinemaai.dto.request.food.FoodItemRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.food.FoodComboResponse;
import com.sba301.cinemaai.dto.response.food.FoodItemResponse;
import com.sba301.cinemaai.entity.FoodCombo;
import com.sba301.cinemaai.entity.FoodItem;
import com.sba301.cinemaai.enums.FoodItemStatus;
import java.util.List;

public interface FoodService {

    List<FoodItemResponse> getActiveItems();

    PageResponse<FoodItemResponse> getActiveItems(int page, int size);

    List<FoodComboResponse> getActiveCombos();

    PageResponse<FoodComboResponse> getActiveCombos(int page, int size);

    List<FoodItemResponse> getAllItems();

    PageResponse<FoodItemResponse> getAllItems(int page, int size);

    List<FoodComboResponse> getAllCombos();

    PageResponse<FoodComboResponse> getAllCombos(int page, int size);

    FoodItemResponse createItem(FoodItemRequest request);

    FoodComboResponse createCombo(FoodComboRequest request);

    FoodItemResponse updateItem(Long id, FoodItemRequest request);

    FoodComboResponse updateCombo(Long id, FoodComboRequest request);

    FoodItemResponse updateItemStatus(Long id, FoodItemStatus status);

    FoodComboResponse updateComboStatus(Long id, FoodItemStatus status);

    FoodItemResponse deleteItem(Long id);

    FoodComboResponse deleteCombo(Long id);

    FoodItem findItem(Long id);

    FoodCombo findCombo(Long id);
}
