package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.food.FoodComboRequest;
import com.sba301.cinemaai.dto.response.food.FoodComboResponse;
import com.sba301.cinemaai.dto.request.food.FoodItemRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.food.FoodItemResponse;
import com.sba301.cinemaai.entity.FoodCombo;
import com.sba301.cinemaai.entity.FoodItem;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.FoodItemStatus;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.mapper.FoodMapper;
import com.sba301.cinemaai.repository.FoodComboRepository;
import com.sba301.cinemaai.repository.FoodItemRepository;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.FoodService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FoodServiceImpl implements FoodService {

    private static final List<FoodItemStatus> SELLABLE_STATUSES = List.of(
            FoodItemStatus.ACTIVE,
            FoodItemStatus.LOW_STOCK
    );

    private final FoodItemRepository foodItemRepository;
    private final FoodComboRepository foodComboRepository;
    private final FoodMapper foodMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<FoodItemResponse> getActiveItems() {
        return foodItemRepository.findByStatusIn(SELLABLE_STATUSES)
                .stream()
                .map(foodMapper::toFoodItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<FoodItemResponse> getActiveItems(int page, int size) {
        return PageResponse.from(foodItemRepository
                .findByStatusIn(SELLABLE_STATUSES, pageable(page, size))
                .map(foodMapper::toFoodItemResponse));
    }

    @Transactional(readOnly = true)
    public List<FoodComboResponse> getActiveCombos() {
        return foodComboRepository.findByStatusIn(SELLABLE_STATUSES)
                .stream()
                .map(foodMapper::toFoodComboResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<FoodComboResponse> getActiveCombos(int page, int size) {
        return PageResponse.from(foodComboRepository
                .findByStatusIn(SELLABLE_STATUSES, pageable(page, size))
                .map(foodMapper::toFoodComboResponse));
    }

    @Transactional(readOnly = true)
    public List<FoodItemResponse> getAllItems() {
        return foodItemRepository.findAll().stream().map(foodMapper::toFoodItemResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<FoodItemResponse> getAllItems(int page, int size) {
        return PageResponse.from(foodItemRepository
                .findAll(pageable(page, size))
                .map(foodMapper::toFoodItemResponse));
    }

    @Transactional(readOnly = true)
    public List<FoodComboResponse> getAllCombos() {
        return foodComboRepository.findAll().stream().map(foodMapper::toFoodComboResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<FoodComboResponse> getAllCombos(int page, int size) {
        return PageResponse.from(foodComboRepository
                .findAll(pageable(page, size))
                .map(foodMapper::toFoodComboResponse));
    }

    @Transactional
    public FoodItemResponse createItem(FoodItemRequest request) {
        FoodItem foodItem = new FoodItem(request.name(), request.description(), request.price());
        applyItemFields(foodItem, request);
        foodItem.setStatus(normalizeStatus(request.status(), FoodItemStatus.ACTIVE));
        FoodItem saved = foodItemRepository.save(foodItem);
        auditLogService.record(AuditActionType.CREATE, "FOOD_ITEM", saved.getId(), saved.getName());
        return foodMapper.toFoodItemResponse(saved);
    }

    @Transactional
    public FoodComboResponse createCombo(FoodComboRequest request) {
        FoodCombo foodCombo = new FoodCombo(request.name(), request.description(), request.price());
        applyComboFields(foodCombo, request);
        foodCombo.setStatus(normalizeStatus(request.status(), FoodItemStatus.ACTIVE));
        FoodCombo saved = foodComboRepository.save(foodCombo);
        auditLogService.record(AuditActionType.CREATE, "FOOD_COMBO", saved.getId(), saved.getName());
        return foodMapper.toFoodComboResponse(saved);
    }

    @Transactional
    public FoodItemResponse updateItem(Long id, FoodItemRequest request) {
        FoodItem foodItem = findItem(id);
        applyItemFields(foodItem, request);
        foodItem.setStatus(normalizeStatus(request.status(), foodItem.getStatus()));
        auditLogService.record(AuditActionType.UPDATE, "FOOD_ITEM", foodItem.getId(), foodItem.getName());
        return foodMapper.toFoodItemResponse(foodItem);
    }

    @Transactional
    public FoodComboResponse updateCombo(Long id, FoodComboRequest request) {
        FoodCombo foodCombo = findCombo(id);
        applyComboFields(foodCombo, request);
        foodCombo.setStatus(normalizeStatus(request.status(), foodCombo.getStatus()));
        auditLogService.record(AuditActionType.UPDATE, "FOOD_COMBO", foodCombo.getId(), foodCombo.getName());
        return foodMapper.toFoodComboResponse(foodCombo);
    }

    @Transactional
    public FoodItemResponse updateItemStatus(Long id, FoodItemStatus status) {
        FoodItem foodItem = findItem(id);
        foodItem.setStatus(normalizeStatus(status, FoodItemStatus.ACTIVE));
        return foodMapper.toFoodItemResponse(foodItem);
    }

    @Transactional
    public FoodComboResponse updateComboStatus(Long id, FoodItemStatus status) {
        FoodCombo foodCombo = findCombo(id);
        foodCombo.setStatus(normalizeStatus(status, FoodItemStatus.ACTIVE));
        return foodMapper.toFoodComboResponse(foodCombo);
    }

    @Transactional
    public FoodItemResponse deleteItem(Long id) {
        FoodItem foodItem = findItem(id);
        foodItem.setStatus(FoodItemStatus.OUT_OF_STOCK);
        auditLogService.record(AuditActionType.DELETE, "FOOD_ITEM", foodItem.getId(), foodItem.getName());
        return foodMapper.toFoodItemResponse(foodItem);
    }

    @Transactional
    public FoodComboResponse deleteCombo(Long id) {
        FoodCombo foodCombo = findCombo(id);
        foodCombo.setStatus(FoodItemStatus.OUT_OF_STOCK);
        auditLogService.record(AuditActionType.DELETE, "FOOD_COMBO", foodCombo.getId(), foodCombo.getName());
        return foodMapper.toFoodComboResponse(foodCombo);
    }

    public FoodItem findItem(Long id) {
        return foodItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Food item not found"));
    }

    public FoodCombo findCombo(Long id) {
        return foodComboRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Food combo not found"));
    }

    private void applyItemFields(FoodItem foodItem, FoodItemRequest request) {
        foodItem.setName(request.name());
        foodItem.setDescription(request.description());
        foodItem.setPrice(request.price());
        foodItem.setImageUrl(request.imageUrl());
    }

    private void applyComboFields(FoodCombo foodCombo, FoodComboRequest request) {
        foodCombo.setName(request.name());
        foodCombo.setDescription(request.description());
        foodCombo.setPrice(request.price());
        foodCombo.setImageUrl(request.imageUrl());
    }

    private FoodItemStatus normalizeStatus(FoodItemStatus requestedStatus, FoodItemStatus fallbackStatus) {
        if (requestedStatus == null) {
            return fallbackStatus == FoodItemStatus.INACTIVE ? FoodItemStatus.OUT_OF_STOCK : fallbackStatus;
        }
        return requestedStatus == FoodItemStatus.INACTIVE ? FoodItemStatus.OUT_OF_STOCK : requestedStatus;
    }

    private PageRequest pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, Sort.by("name").ascending());
    }
}
