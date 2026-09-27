package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.food.StockAdjustmentRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.food.FoodInventoryResponse;
import com.sba301.cinemaai.dto.response.food.FoodInventoryTransactionResponse;
import com.sba301.cinemaai.enums.FoodStockStatus;
import java.util.List;

public interface FoodInventoryService {

    List<FoodInventoryResponse> getInventoriesByCinema(Long cinemaId);

    List<FoodInventoryResponse> getInventoriesByFoodItem(Long foodItemId);

    FoodInventoryResponse adjustStock(StockAdjustmentRequest request, String currentUsername);

    PageResponse<FoodInventoryTransactionResponse> getTransactions(Long cinemaId, Long foodItemId, int page, int size);

    int getAvailableStock(Long foodItemId, Long cinemaId);

    FoodStockStatus calculateItemStockStatus(Long foodItemId, Long cinemaId);

    int calculateAvailableComboStock(Long comboId, Long cinemaId);

    FoodStockStatus calculateComboStockStatus(Long comboId, Long cinemaId);

    void deductStockForOrder(Long cinemaId, Long foodItemId, Long foodComboId, int quantity, String orderReference);
}
