package com.cinemaai.catalog.service.impl;

import com.cinemaai.catalog.dto.request.product.ProductBundleRequest;
import com.cinemaai.catalog.dto.request.product.ProductRequest;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.product.ProductBundleResponse;
import com.cinemaai.catalog.dto.response.product.ProductResponse;
import com.cinemaai.catalog.entity.ProductBundle;
import com.cinemaai.catalog.entity.Product;
import com.cinemaai.catalog.enums.AuditActionType;
import com.cinemaai.catalog.enums.ProductStatus;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.mapper.ProductMapper;
import com.cinemaai.catalog.repository.ProductBundleRepository;
import com.cinemaai.catalog.repository.ProductRepository;
import com.cinemaai.catalog.service.AuditLogService;
import com.cinemaai.catalog.service.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final List<ProductStatus> SELLABLE_STATUSES = List.of(
            ProductStatus.ACTIVE,
            ProductStatus.LOW_STOCK
    );

    private final ProductRepository productRepository;
    private final ProductBundleRepository productBundleRepository;
    private final ProductMapper productMapper;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<ProductResponse> getActiveItems() {
        return productRepository.findByStatusIn(SELLABLE_STATUSES)
                .stream()
                .map(productMapper::toFoodItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getActiveItems(int page, int size) {
        return PageResponse.from(productRepository
                .findByStatusIn(SELLABLE_STATUSES, pageable(page, size))
                .map(productMapper::toFoodItemResponse));
    }

    @Transactional(readOnly = true)
    public List<ProductBundleResponse> getActiveCombos() {
        return productBundleRepository.findByStatusIn(SELLABLE_STATUSES)
                .stream()
                .map(productMapper::toFoodComboResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductBundleResponse> getActiveCombos(int page, int size) {
        return PageResponse.from(productBundleRepository
                .findByStatusIn(SELLABLE_STATUSES, pageable(page, size))
                .map(productMapper::toFoodComboResponse));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllItems() {
        return productRepository.findAll().stream().map(productMapper::toFoodItemResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllItems(int page, int size) {
        return PageResponse.from(productRepository
                .findAll(pageable(page, size))
                .map(productMapper::toFoodItemResponse));
    }

    @Transactional(readOnly = true)
    public List<ProductBundleResponse> getAllCombos() {
        return productBundleRepository.findAll().stream().map(productMapper::toFoodComboResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductBundleResponse> getAllCombos(int page, int size) {
        return PageResponse.from(productBundleRepository
                .findAll(pageable(page, size))
                .map(productMapper::toFoodComboResponse));
    }

    @Transactional
    public ProductResponse createItem(ProductRequest request) {
        Product foodItem = new Product(request.name(), request.description(), request.price());
        applyItemFields(foodItem, request);
        foodItem.setStatus(normalizeStatus(request.status(), ProductStatus.ACTIVE));
        Product saved = productRepository.save(foodItem);
        auditLogService.record(AuditActionType.CREATE, "FOOD_ITEM", saved.getId(), saved.getName());
        return productMapper.toFoodItemResponse(saved);
    }

    @Transactional
    public ProductBundleResponse createCombo(ProductBundleRequest request) {
        ProductBundle foodCombo = new ProductBundle(request.name(), request.description(), request.price());
        applyComboFields(foodCombo, request);
        foodCombo.setStatus(normalizeStatus(request.status(), ProductStatus.ACTIVE));
        ProductBundle saved = productBundleRepository.save(foodCombo);
        auditLogService.record(AuditActionType.CREATE, "FOOD_COMBO", saved.getId(), saved.getName());
        return productMapper.toFoodComboResponse(saved);
    }

    @Transactional
    public ProductResponse updateItem(Long id, ProductRequest request) {
        Product foodItem = findItem(id);
        applyItemFields(foodItem, request);
        foodItem.setStatus(normalizeStatus(request.status(), foodItem.getStatus()));
        auditLogService.record(AuditActionType.UPDATE, "FOOD_ITEM", foodItem.getId(), foodItem.getName());
        return productMapper.toFoodItemResponse(foodItem);
    }

    @Transactional
    public ProductBundleResponse updateCombo(Long id, ProductBundleRequest request) {
        ProductBundle foodCombo = findCombo(id);
        applyComboFields(foodCombo, request);
        foodCombo.setStatus(normalizeStatus(request.status(), foodCombo.getStatus()));
        auditLogService.record(AuditActionType.UPDATE, "FOOD_COMBO", foodCombo.getId(), foodCombo.getName());
        return productMapper.toFoodComboResponse(foodCombo);
    }

    @Transactional
    public ProductResponse updateItemStatus(Long id, ProductStatus status) {
        Product foodItem = findItem(id);
        foodItem.setStatus(normalizeStatus(status, ProductStatus.ACTIVE));
        return productMapper.toFoodItemResponse(foodItem);
    }

    @Transactional
    public ProductBundleResponse updateComboStatus(Long id, ProductStatus status) {
        ProductBundle foodCombo = findCombo(id);
        foodCombo.setStatus(normalizeStatus(status, ProductStatus.ACTIVE));
        return productMapper.toFoodComboResponse(foodCombo);
    }

    @Transactional
    public ProductResponse deleteItem(Long id) {
        Product foodItem = findItem(id);
        foodItem.setStatus(ProductStatus.OUT_OF_STOCK);
        auditLogService.record(AuditActionType.DELETE, "FOOD_ITEM", foodItem.getId(), foodItem.getName());
        return productMapper.toFoodItemResponse(foodItem);
    }

    @Transactional
    public ProductBundleResponse deleteCombo(Long id) {
        ProductBundle foodCombo = findCombo(id);
        foodCombo.setStatus(ProductStatus.OUT_OF_STOCK);
        auditLogService.record(AuditActionType.DELETE, "FOOD_COMBO", foodCombo.getId(), foodCombo.getName());
        return productMapper.toFoodComboResponse(foodCombo);
    }

    public Product findItem(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Food item not found"));
    }

    public ProductBundle findCombo(Long id) {
        return productBundleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Food combo not found"));
    }

    private void applyItemFields(Product foodItem, ProductRequest request) {
        foodItem.setName(request.name());
        foodItem.setDescription(request.description());
        foodItem.setPrice(request.price());
        foodItem.setImageUrl(request.imageUrl());
        if (request.categoryId() != null) {
            foodItem.setCategoryId(request.categoryId());
        }
    }

    private void applyComboFields(ProductBundle foodCombo, ProductBundleRequest request) {
        foodCombo.setName(request.name());
        foodCombo.setDescription(request.description());
        foodCombo.setPrice(request.price());
        foodCombo.setImageUrl(request.imageUrl());
    }

    private ProductStatus normalizeStatus(ProductStatus requestedStatus, ProductStatus fallbackStatus) {
        if (requestedStatus == null) {
            return fallbackStatus == ProductStatus.INACTIVE ? ProductStatus.OUT_OF_STOCK : fallbackStatus;
        }
        return requestedStatus == ProductStatus.INACTIVE ? ProductStatus.OUT_OF_STOCK : requestedStatus;
    }

    private PageRequest pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, Sort.by("name").ascending());
    }
}
