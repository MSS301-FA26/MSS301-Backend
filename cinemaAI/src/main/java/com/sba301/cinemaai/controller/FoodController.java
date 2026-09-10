package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.food.FoodComboResponse;
import com.sba301.cinemaai.dto.response.food.FoodItemResponse;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.service.FoodService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/foods")
@Tag(name = "Food & Combo")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping("/items")
    public ApiResponse<PageResponse<FoodItemResponse>> getItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(foodService.getActiveItems(page, size));
    }

    @GetMapping("/combos")
    public ApiResponse<PageResponse<FoodComboResponse>> getCombos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(foodService.getActiveCombos(page, size));
    }
}
