package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.food.FoodCategoryRequest;
import com.sba301.cinemaai.dto.response.food.FoodCategoryResponse;
import com.sba301.cinemaai.entity.FoodCategory;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.ConflictException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.FoodCategoryRepository;
import com.sba301.cinemaai.repository.FoodComboRepository;
import com.sba301.cinemaai.repository.FoodItemRepository;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.FoodCategoryService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FoodCategoryServiceImpl implements FoodCategoryService {

    private final FoodCategoryRepository foodCategoryRepository;
    private final FoodItemRepository foodItemRepository;
    private final FoodComboRepository foodComboRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<FoodCategoryResponse> getAllCategories() {
        return foodCategoryRepository.findByDeletedAtIsNullOrderBySortOrderAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FoodCategoryResponse getCategoryById(Long id) {
        FoodCategory category = foodCategoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Food category not found: " + id));
        return toResponse(category);
    }

    @Override
    @Transactional
    public FoodCategoryResponse createCategory(FoodCategoryRequest request) {
        String cleanCode = request.code().trim().toUpperCase();
        if (foodCategoryRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new ConflictException("Category code already exists: " + cleanCode);
        }

        FoodCategory category = new FoodCategory(
                cleanCode,
                request.name().trim(),
                request.description() != null ? request.description().trim() : null,
                request.sortOrder() != null ? request.sortOrder() : 0
        );
        category.setImageUrl(request.imageUrl());
        if (request.status() != null && !request.status().isBlank()) {
            category.setStatus(request.status().trim().toUpperCase());
        }

        FoodCategory saved = foodCategoryRepository.save(category);
        auditLogService.record(AuditActionType.CREATE, "FOOD_CATEGORY", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public FoodCategoryResponse updateCategory(Long id, FoodCategoryRequest request) {
        FoodCategory category = foodCategoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Food category not found: " + id));

        String cleanCode = request.code().trim().toUpperCase();
        if (foodCategoryRepository.existsByCodeIgnoreCaseAndIdNot(cleanCode, id)) {
            throw new ConflictException("Category code already exists: " + cleanCode);
        }

        category.setCode(cleanCode);
        category.setName(request.name().trim());
        category.setDescription(request.description() != null ? request.description().trim() : null);
        category.setImageUrl(request.imageUrl());
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        if (request.status() != null && !request.status().isBlank()) {
            category.setStatus(request.status().trim().toUpperCase());
        }

        auditLogService.record(AuditActionType.UPDATE, "FOOD_CATEGORY", category.getId(), category.getName());
        return toResponse(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        FoodCategory category = foodCategoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Food category not found: " + id));

        long itemCount = foodItemRepository.countByCategoryIdAndDeletedAtIsNull(id);
        long comboCount = foodComboRepository.countByCategoryIdAndDeletedAtIsNull(id);
        if (itemCount > 0 || comboCount > 0) {
            throw new BadRequestException("Không thể xóa danh mục đang có " + (itemCount + comboCount) + " món/combo đang sử dụng.");
        }

        category.setDeletedAt(LocalDateTime.now());
        auditLogService.record(AuditActionType.DELETE, "FOOD_CATEGORY", category.getId(), category.getName());
    }

    @Override
    @Transactional
    public FoodCategoryResponse restoreCategory(Long id) {
        FoodCategory category = foodCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Food category not found: " + id));

        category.setDeletedAt(null);
        auditLogService.record(AuditActionType.UPDATE, "FOOD_CATEGORY", category.getId(), "Restored: " + category.getName());
        return toResponse(category);
    }

    private FoodCategoryResponse toResponse(FoodCategory category) {
        long items = foodItemRepository.countByCategoryIdAndDeletedAtIsNull(category.getId());
        long combos = foodComboRepository.countByCategoryIdAndDeletedAtIsNull(category.getId());
        return new FoodCategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDescription(),
                category.getImageUrl(),
                category.getSortOrder(),
                category.getStatus(),
                items + combos
        );
    }
}
