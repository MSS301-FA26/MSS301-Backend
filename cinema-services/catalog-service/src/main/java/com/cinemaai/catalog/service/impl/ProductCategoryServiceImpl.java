package com.cinemaai.catalog.service.impl;

import com.cinemaai.catalog.dto.request.product.ProductCategoryRequest;
import com.cinemaai.catalog.dto.response.product.ProductCategoryResponse;
import com.cinemaai.catalog.entity.ProductCategory;
import com.cinemaai.catalog.exception.BadRequestException;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.repository.ProductCategoryRepository;
import com.cinemaai.catalog.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryResponse> getActiveCategories() {
        checkAndSeedDefaults();
        return repository.findByStatusOrderBySortOrderAscNameAsc("ACTIVE")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryResponse> getAllCategories() {
        checkAndSeedDefaults();
        return repository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCategoryResponse getCategoryById(Long id) {
        ProductCategory category = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Food category not found with id: " + id));
        return toResponse(category);
    }

    @Override
    @Transactional
    public ProductCategoryResponse createCategory(ProductCategoryRequest request) {
        String code = request.code() != null ? request.code().trim().toUpperCase() : "";
        if (code.isBlank()) {
            throw new BadRequestException("Category code is required");
        }
        if (repository.existsByCode(code)) {
            throw new BadRequestException("Category code already exists: " + code);
        }
        ProductCategory category = new ProductCategory(
                code,
                request.name().trim(),
                request.description(),
                request.sortOrder(),
                request.status() != null ? request.status() : "ACTIVE"
        );
        return toResponse(repository.save(category));
    }

    @Override
    @Transactional
    public ProductCategoryResponse updateCategory(Long id, ProductCategoryRequest request) {
        ProductCategory category = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Food category not found with id: " + id));

        String code = request.code() != null ? request.code().trim().toUpperCase() : category.getCode();
        if (!code.equalsIgnoreCase(category.getCode()) && repository.existsByCode(code)) {
            throw new BadRequestException("Category code already exists: " + code);
        }

        category.setCode(code);
        if (request.name() != null && !request.name().isBlank()) {
            category.setName(request.name().trim());
        }
        if (request.description() != null) {
            category.setDescription(request.description());
        }
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        if (request.status() != null && !request.status().isBlank()) {
            category.setStatus(request.status());
        }
        return toResponse(repository.save(category));
    }

    @Override
    @Transactional
    public ProductCategoryResponse deleteCategory(Long id) {
        ProductCategory category = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Food category not found with id: " + id));
        category.setStatus("INACTIVE");
        return toResponse(repository.save(category));
    }

    @Override
    @Transactional
    public ProductCategoryResponse restoreCategory(Long id) {
        ProductCategory category = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Food category not found with id: " + id));
        category.setStatus("ACTIVE");
        return toResponse(repository.save(category));
    }

    private ProductCategoryResponse toResponse(ProductCategory cat) {
        return new ProductCategoryResponse(
                cat.getId(),
                cat.getCode(),
                cat.getName(),
                cat.getDescription(),
                cat.getSortOrder(),
                cat.getStatus()
        );
    }

    private void checkAndSeedDefaults() {
        if (repository.count() == 0) {
            log.info("Seeding initial food categories into database...");
            repository.saveAll(List.of(
                    new ProductCategory("COMBO", "Combo Tiết Kiệm", "Các gói combo bắp nước siêu tiết kiệm kèm ưu đãi", 1, "ACTIVE"),
                    new ProductCategory("POPCORN", "Bắp Rang Bơ", "Bắp rang bơ nóng hổi thơm ngon các vị bơ, phô mai, caramel, socola", 2, "ACTIVE"),
                    new ProductCategory("DRINK", "Nước Giải Khát", "Nước ngọt có ga mát lạnh, trà trái cây và nước suối tinh khiết", 3, "ACTIVE"),
                    new ProductCategory("SNACK", "Đồ Ăn Vặt", "Khoai tây lắc phô mai, xúc xích nướng thơm lừng nóng hổi", 4, "ACTIVE")
            ));
        }
    }
}
