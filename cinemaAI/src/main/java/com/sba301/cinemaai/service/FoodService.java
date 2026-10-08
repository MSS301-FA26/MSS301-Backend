package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.food.FoodComboRequest;
import com.sba301.cinemaai.dto.request.food.FoodItemRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.food.FoodComboResponse;
import com.sba301.cinemaai.dto.response.food.FoodItemResponse;
import com.sba301.cinemaai.dto.response.food.FoodPriceHistoryResponse;
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

    PageResponse<FoodItemResponse> searchItems(
            String search,
            Long categoryId,
            FoodItemStatus status,
            boolean includeDeleted,
            int page,
            int size,
            String sort
    );

    PageResponse<FoodComboResponse> searchCombos(
            String search,
            Long categoryId,
            FoodItemStatus status,
            boolean includeDeleted,
            int page,
            int size,
            String sort
    );

    FoodItemResponse createItem(FoodItemRequest request);

    FoodComboResponse createCombo(FoodComboRequest request);

    FoodItemResponse updateItem(Long id, FoodItemRequest request);

    FoodComboResponse updateCombo(Long id, FoodComboRequest request);

    FoodItemResponse updateItemStatus(Long id, FoodItemStatus status);

    FoodComboResponse updateComboStatus(Long id, FoodItemStatus status);

    FoodItemResponse deleteItem(Long id);

    FoodComboResponse deleteCombo(Long id);

    FoodItemResponse restoreItem(Long id);

    FoodComboResponse restoreCombo(Long id);

    FoodItemResponse duplicateItem(Long id);

    FoodComboResponse duplicateCombo(Long id);

    void bulkUpdateStatus(List<Long> ids, String kind, FoodItemStatus status);

    void bulkDelete(List<Long> ids, String kind);

    List<FoodPriceHistoryResponse> getPriceHistory(Long id, String kind);

    FoodItem findItem(Long id);

    FoodCombo findCombo(Long id);
}
