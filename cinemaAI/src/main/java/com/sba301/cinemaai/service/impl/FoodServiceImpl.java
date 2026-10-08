package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.food.ComboItemRequest;
import com.sba301.cinemaai.dto.request.food.FoodComboRequest;
import com.sba301.cinemaai.dto.request.food.FoodItemRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.food.FoodComboResponse;
import com.sba301.cinemaai.dto.response.food.FoodItemResponse;
import com.sba301.cinemaai.dto.response.food.FoodPriceHistoryResponse;
import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.entity.FoodCategory;
import com.sba301.cinemaai.entity.FoodCombo;
import com.sba301.cinemaai.entity.FoodComboItem;
import com.sba301.cinemaai.entity.FoodInventory;
import com.sba301.cinemaai.entity.FoodInventoryTransaction;
import com.sba301.cinemaai.entity.FoodItem;
import com.sba301.cinemaai.entity.FoodPriceHistory;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.FoodInventoryTxType;
import com.sba301.cinemaai.enums.FoodItemStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.ConflictException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.mapper.FoodMapper;
import com.sba301.cinemaai.repository.CinemaRepository;
import com.sba301.cinemaai.repository.FoodCategoryRepository;
import com.sba301.cinemaai.repository.FoodComboItemRepository;
import com.sba301.cinemaai.repository.FoodComboRepository;
import com.sba301.cinemaai.repository.FoodInventoryRepository;
import com.sba301.cinemaai.repository.FoodInventoryTransactionRepository;
import com.sba301.cinemaai.repository.FoodItemRepository;
import com.sba301.cinemaai.repository.FoodPriceHistoryRepository;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.FoodService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodServiceImpl implements FoodService {

    private static final List<FoodItemStatus> SELLABLE_STATUSES = List.of(
            FoodItemStatus.ACTIVE,
            FoodItemStatus.LOW_STOCK
    );

    private final FoodItemRepository foodItemRepository;
    private final FoodComboRepository foodComboRepository;
    private final FoodCategoryRepository foodCategoryRepository;
    private final FoodComboItemRepository foodComboItemRepository;
    private final FoodInventoryRepository foodInventoryRepository;
    private final FoodInventoryTransactionRepository foodInventoryTransactionRepository;
    private final FoodPriceHistoryRepository foodPriceHistoryRepository;
    private final CinemaRepository cinemaRepository;
    private final FoodMapper foodMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<FoodItemResponse> getActiveItems() {
        return foodItemRepository.findByStatusIn(SELLABLE_STATUSES)
                .stream()
                .filter(item -> !item.isDeleted())
                .map(foodMapper::toFoodItemResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FoodItemResponse> getActiveItems(int page, int size) {
        return PageResponse.from(foodItemRepository
                .findByStatusIn(SELLABLE_STATUSES, pageable(page, size))
                .map(foodMapper::toFoodItemResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodComboResponse> getActiveCombos() {
        return foodComboRepository.findByStatusIn(SELLABLE_STATUSES)
                .stream()
                .filter(combo -> !combo.isDeleted())
                .map(foodMapper::toFoodComboResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FoodComboResponse> getActiveCombos(int page, int size) {
        return PageResponse.from(foodComboRepository
                .findByStatusIn(SELLABLE_STATUSES, pageable(page, size))
                .map(foodMapper::toFoodComboResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItemResponse> getAllItems() {
        return foodItemRepository.findByDeletedAtIsNull()
                .stream()
                .map(foodMapper::toFoodItemResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FoodItemResponse> getAllItems(int page, int size) {
        return PageResponse.from(foodItemRepository
                .findByDeletedAtIsNull(pageable(page, size))
                .map(foodMapper::toFoodItemResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodComboResponse> getAllCombos() {
        return foodComboRepository.findByDeletedAtIsNull()
                .stream()
                .map(foodMapper::toFoodComboResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FoodComboResponse> getAllCombos(int page, int size) {
        return PageResponse.from(foodComboRepository
                .findByDeletedAtIsNull(pageable(page, size))
                .map(foodMapper::toFoodComboResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FoodItemResponse> searchItems(
            String search,
            Long categoryId,
            FoodItemStatus status,
            boolean includeDeleted,
            int page,
            int size,
            String sort
    ) {
        PageRequest pr = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), resolveSort(sort));
        Page<FoodItem> pageResult = foodItemRepository.searchItems(
                search != null && !search.isBlank() ? search.trim() : null,
                categoryId,
                status,
                includeDeleted,
                pr
        );
        return PageResponse.from(pageResult.map(foodMapper::toFoodItemResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FoodComboResponse> searchCombos(
            String search,
            Long categoryId,
            FoodItemStatus status,
            boolean includeDeleted,
            int page,
            int size,
            String sort
    ) {
        PageRequest pr = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), resolveSort(sort));
        Page<FoodCombo> pageResult = foodComboRepository.searchCombos(
                search != null && !search.isBlank() ? search.trim() : null,
                categoryId,
                status,
                includeDeleted,
                pr
        );
        return PageResponse.from(pageResult.map(foodMapper::toFoodComboResponse));
    }

    @Override
    @Transactional
    public FoodItemResponse createItem(FoodItemRequest request) {
        String cleanSku = (request.sku() != null && !request.sku().isBlank())
                ? request.sku().trim().toUpperCase()
                : generateUniqueItemSku("POP-" + (request.name().length() > 3 ? request.name().substring(0, 3).toUpperCase() : "ITM"));

        if (foodItemRepository.existsBySkuIgnoreCase(cleanSku)) {
            throw new ConflictException("Mã SKU đã tồn tại: " + cleanSku);
        }

        FoodItem foodItem = new FoodItem(request.name().trim(), request.description(), request.price());
        foodItem.setSku(cleanSku);
        applyItemFields(foodItem, request);
        foodItem.setStatus(normalizeStatus(request.status(), FoodItemStatus.ACTIVE));

        FoodItem saved = foodItemRepository.save(foodItem);

        // Optional initial stock seeding across cinemas
        if (request.initialStock() != null && request.initialStock() > 0) {
            initItemStock(saved, request.initialStock(), request.cinemaId());
        }

        auditLogService.record(AuditActionType.CREATE, "FOOD_ITEM", saved.getId(), saved.getName() + " (SKU: " + saved.getSku() + ")");
        return foodMapper.toFoodItemResponse(saved);
    }

    @Override
    @Transactional
    public FoodComboResponse createCombo(FoodComboRequest request) {
        String cleanSku = (request.sku() != null && !request.sku().isBlank())
                ? request.sku().trim().toUpperCase()
                : generateUniqueComboSku("CMB-" + (request.name().length() > 3 ? request.name().substring(0, 3).toUpperCase() : "SET"));

        if (foodComboRepository.existsBySkuIgnoreCase(cleanSku)) {
            throw new ConflictException("Mã SKU đã tồn tại: " + cleanSku);
        }

        FoodCombo foodCombo = new FoodCombo(request.name().trim(), request.description(), request.price());
        foodCombo.setSku(cleanSku);
        applyComboFields(foodCombo, request);
        foodCombo.setStatus(normalizeStatus(request.status(), FoodItemStatus.ACTIVE));

        FoodCombo saved = foodComboRepository.save(foodCombo);

        // Save combo recipe items if provided
        if (request.items() != null && !request.items().isEmpty()) {
            saveComboRecipeItems(saved, request.items());
        }

        auditLogService.record(AuditActionType.CREATE, "FOOD_COMBO", saved.getId(), saved.getName() + " (SKU: " + saved.getSku() + ")");
        return foodMapper.toFoodComboResponse(saved);
    }

    @Override
    @Transactional
    public FoodItemResponse updateItem(Long id, FoodItemRequest request) {
        FoodItem foodItem = findItem(id);

        if (request.sku() != null && !request.sku().isBlank()) {
            String cleanSku = request.sku().trim().toUpperCase();
            if (foodItemRepository.existsBySkuIgnoreCaseAndIdNot(cleanSku, id)) {
                throw new ConflictException("Mã SKU đã tồn tại: " + cleanSku);
            }
            foodItem.setSku(cleanSku);
        }

        // Check price history
        if (request.price() != null && foodItem.getPrice().compareTo(request.price()) != 0) {
            foodPriceHistoryRepository.save(new FoodPriceHistory(foodItem, null, foodItem.getPrice(), request.price(), "ADMIN"));
        }

        applyItemFields(foodItem, request);
        foodItem.setStatus(normalizeStatus(request.status(), foodItem.getStatus()));

        auditLogService.record(AuditActionType.UPDATE, "FOOD_ITEM", foodItem.getId(), foodItem.getName());
        return foodMapper.toFoodItemResponse(foodItem);
    }

    @Override
    @Transactional
    public FoodComboResponse updateCombo(Long id, FoodComboRequest request) {
        FoodCombo foodCombo = findCombo(id);

        if (request.sku() != null && !request.sku().isBlank()) {
            String cleanSku = request.sku().trim().toUpperCase();
            if (foodComboRepository.existsBySkuIgnoreCaseAndIdNot(cleanSku, id)) {
                throw new ConflictException("Mã SKU đã tồn tại: " + cleanSku);
            }
            foodCombo.setSku(cleanSku);
        }

        // Check price history
        if (request.price() != null && foodCombo.getPrice().compareTo(request.price()) != 0) {
            foodPriceHistoryRepository.save(new FoodPriceHistory(null, foodCombo, foodCombo.getPrice(), request.price(), "ADMIN"));
        }

        applyComboFields(foodCombo, request);
        foodCombo.setStatus(normalizeStatus(request.status(), foodCombo.getStatus()));

        // Update recipe items if specified
        if (request.items() != null) {
            foodComboItemRepository.deleteByComboId(foodCombo.getId());
            if (!request.items().isEmpty()) {
                saveComboRecipeItems(foodCombo, request.items());
            }
        }

        auditLogService.record(AuditActionType.UPDATE, "FOOD_COMBO", foodCombo.getId(), foodCombo.getName());
        return foodMapper.toFoodComboResponse(foodCombo);
    }

    @Override
    @Transactional
    public FoodItemResponse updateItemStatus(Long id, FoodItemStatus status) {
        FoodItem foodItem = findItem(id);
        foodItem.setStatus(normalizeStatus(status, FoodItemStatus.ACTIVE));
        return foodMapper.toFoodItemResponse(foodItem);
    }

    @Override
    @Transactional
    public FoodComboResponse updateComboStatus(Long id, FoodItemStatus status) {
        FoodCombo foodCombo = findCombo(id);
        foodCombo.setStatus(normalizeStatus(status, FoodItemStatus.ACTIVE));
        return foodMapper.toFoodComboResponse(foodCombo);
    }

    @Override
    @Transactional
    public FoodItemResponse deleteItem(Long id) {
        FoodItem foodItem = findItem(id);
        foodItem.setDeletedAt(LocalDateTime.now());
        foodItem.setStatus(FoodItemStatus.ARCHIVED);
        auditLogService.record(AuditActionType.DELETE, "FOOD_ITEM", foodItem.getId(), foodItem.getName());
        return foodMapper.toFoodItemResponse(foodItem);
    }

    @Override
    @Transactional
    public FoodComboResponse deleteCombo(Long id) {
        FoodCombo foodCombo = findCombo(id);
        foodCombo.setDeletedAt(LocalDateTime.now());
        foodCombo.setStatus(FoodItemStatus.ARCHIVED);
        auditLogService.record(AuditActionType.DELETE, "FOOD_COMBO", foodCombo.getId(), foodCombo.getName());
        return foodMapper.toFoodComboResponse(foodCombo);
    }

    @Override
    @Transactional
    public FoodItemResponse restoreItem(Long id) {
        FoodItem foodItem = findItem(id);
        foodItem.setDeletedAt(null);
        foodItem.setStatus(FoodItemStatus.ACTIVE);
        auditLogService.record(AuditActionType.UPDATE, "FOOD_ITEM", foodItem.getId(), "Khôi phục: " + foodItem.getName());
        return foodMapper.toFoodItemResponse(foodItem);
    }

    @Override
    @Transactional
    public FoodComboResponse restoreCombo(Long id) {
        FoodCombo foodCombo = findCombo(id);
        foodCombo.setDeletedAt(null);
        foodCombo.setStatus(FoodItemStatus.ACTIVE);
        auditLogService.record(AuditActionType.UPDATE, "FOOD_COMBO", foodCombo.getId(), "Khôi phục: " + foodCombo.getName());
        return foodMapper.toFoodComboResponse(foodCombo);
    }

    @Override
    @Transactional
    public FoodItemResponse duplicateItem(Long id) {
        FoodItem original = findItem(id);
        String newSku = generateUniqueItemSku(original.getSku() + "-COPY");

        FoodItem copy = new FoodItem(original.getName() + " (Bản sao)", original.getDescription(), original.getPrice());
        copy.setSku(newSku);
        copy.setCategory(original.getCategory());
        copy.setCostPrice(original.getCostPrice());
        copy.setImageUrl(original.getImageUrl());
        copy.setStatus(FoodItemStatus.DRAFT);
        copy.setStockTracking(original.isStockTracking());
        copy.setLowStockThreshold(original.getLowStockThreshold());

        FoodItem saved = foodItemRepository.save(copy);
        auditLogService.record(AuditActionType.CREATE, "FOOD_ITEM", saved.getId(), "Nhân bản từ ID " + id + ": " + saved.getName());
        return foodMapper.toFoodItemResponse(saved);
    }

    @Override
    @Transactional
    public FoodComboResponse duplicateCombo(Long id) {
        FoodCombo original = findCombo(id);
        String newSku = generateUniqueComboSku(original.getSku() + "-COPY");

        FoodCombo copy = new FoodCombo(original.getName() + " (Bản sao)", original.getDescription(), original.getPrice());
        copy.setSku(newSku);
        copy.setCategory(original.getCategory());
        copy.setCostPrice(original.getCostPrice());
        copy.setImageUrl(original.getImageUrl());
        copy.setStatus(FoodItemStatus.DRAFT);

        FoodCombo saved = foodComboRepository.save(copy);

        // Copy recipe items
        List<FoodComboItem> originalItems = foodComboItemRepository.findByComboId(original.getId());
        for (FoodComboItem item : originalItems) {
            foodComboItemRepository.save(new FoodComboItem(saved, item.getFoodItem(), item.getQuantity()));
        }

        auditLogService.record(AuditActionType.CREATE, "FOOD_COMBO", saved.getId(), "Nhân bản từ Combo ID " + id + ": " + saved.getName());
        return foodMapper.toFoodComboResponse(saved);
    }

    @Override
    @Transactional
    public void bulkUpdateStatus(List<Long> ids, String kind, FoodItemStatus status) {
        if (ids == null || ids.isEmpty()) return;
        if ("combo".equalsIgnoreCase(kind)) {
            for (Long id : ids) {
                foodComboRepository.findById(id).ifPresent(c -> c.setStatus(status));
            }
        } else {
            for (Long id : ids) {
                foodItemRepository.findById(id).ifPresent(i -> i.setStatus(status));
            }
        }
    }

    @Override
    @Transactional
    public void bulkDelete(List<Long> ids, String kind) {
        if (ids == null || ids.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        if ("combo".equalsIgnoreCase(kind)) {
            for (Long id : ids) {
                foodComboRepository.findById(id).ifPresent(c -> {
                    c.setDeletedAt(now);
                    c.setStatus(FoodItemStatus.ARCHIVED);
                });
            }
        } else {
            for (Long id : ids) {
                foodItemRepository.findById(id).ifPresent(i -> {
                    i.setDeletedAt(now);
                    i.setStatus(FoodItemStatus.ARCHIVED);
                });
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodPriceHistoryResponse> getPriceHistory(Long id, String kind) {
        if ("combo".equalsIgnoreCase(kind)) {
            return foodPriceHistoryRepository.findByFoodComboIdOrderByChangedAtDesc(id)
                    .stream()
                    .map(this::toPriceHistoryResponse)
                    .toList();
        }
        return foodPriceHistoryRepository.findByFoodItemIdOrderByChangedAtDesc(id)
                .stream()
                .map(this::toPriceHistoryResponse)
                .toList();
    }

    @Override
    public FoodItem findItem(Long id) {
        return foodItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Food item not found: " + id));
    }

    @Override
    public FoodCombo findCombo(Long id) {
        return foodComboRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Food combo not found: " + id));
    }

    private void applyItemFields(FoodItem foodItem, FoodItemRequest request) {
        foodItem.setName(request.name().trim());
        foodItem.setDescription(request.description() != null ? request.description().trim() : null);
        foodItem.setPrice(request.price());
        foodItem.setImageUrl(request.imageUrl());
        if (request.costPrice() != null) {
            foodItem.setCostPrice(request.costPrice());
        }
        if (request.stockTracking() != null) {
            foodItem.setStockTracking(request.stockTracking());
        }
        if (request.lowStockThreshold() != null) {
            foodItem.setLowStockThreshold(request.lowStockThreshold());
        }
        if (request.categoryId() != null) {
            FoodCategory cat = foodCategoryRepository.findByIdAndDeletedAtIsNull(request.categoryId()).orElse(null);
            foodItem.setCategory(cat);
        }
    }

    private void applyComboFields(FoodCombo foodCombo, FoodComboRequest request) {
        foodCombo.setName(request.name().trim());
        foodCombo.setDescription(request.description() != null ? request.description().trim() : null);
        foodCombo.setPrice(request.price());
        foodCombo.setImageUrl(request.imageUrl());
        if (request.costPrice() != null) {
            foodCombo.setCostPrice(request.costPrice());
        }
        if (request.categoryId() != null) {
            FoodCategory cat = foodCategoryRepository.findByIdAndDeletedAtIsNull(request.categoryId()).orElse(null);
            foodCombo.setCategory(cat);
        }
    }

    private void saveComboRecipeItems(FoodCombo combo, List<ComboItemRequest> items) {
        Set<Long> itemIds = new HashSet<>();
        for (ComboItemRequest cir : items) {
            if (cir.foodItemId() == null || cir.quantity() <= 0) {
                continue;
            }
            if (itemIds.contains(cir.foodItemId())) {
                continue; // Prevent duplicate entries in recipe
            }
            itemIds.add(cir.foodItemId());

            FoodItem foodItem = findItem(cir.foodItemId());
            if (foodItem.isDeleted()) {
                throw new BadRequestException("Không thể thêm món đã bị xóa vào combo: " + foodItem.getName());
            }
            foodComboItemRepository.save(new FoodComboItem(combo, foodItem, cir.quantity()));
        }
    }

    private void initItemStock(FoodItem item, int stock, Long cinemaId) {
        if (cinemaId != null) {
            cinemaRepository.findById(cinemaId).ifPresent(cinema -> recordStockInit(cinema, item, stock));
        } else {
            List<Cinema> cinemas = cinemaRepository.findAll();
            for (Cinema cinema : cinemas) {
                recordStockInit(cinema, item, stock);
            }
        }
    }

    private void recordStockInit(Cinema cinema, FoodItem item, int stock) {
        FoodInventory inv = foodInventoryRepository
                .findByCinemaIdAndFoodItemId(cinema.getId(), item.getId())
                .orElseGet(() -> new FoodInventory(cinema, item, 0, item.getLowStockThreshold()));

        int before = inv.getQuantity();
        inv.setQuantity(stock);
        foodInventoryRepository.save(inv);

        foodInventoryTransactionRepository.save(new FoodInventoryTransaction(
                cinema,
                item,
                FoodInventoryTxType.IMPORT,
                stock,
                before,
                stock,
                "Khởi tạo tồn kho ban đầu khi tạo món",
                null,
                "ADMIN"
        ));
    }

    private String generateUniqueItemSku(String base) {
        String clean = base.replaceAll("[^A-Za-z0-9_-]", "").toUpperCase();
        if (clean.length() > 40) clean = clean.substring(0, 40);
        String candidate = clean;
        int counter = 1;
        while (foodItemRepository.existsBySkuIgnoreCase(candidate)) {
            candidate = clean + "-" + counter;
            counter++;
        }
        return candidate;
    }

    private String generateUniqueComboSku(String base) {
        String clean = base.replaceAll("[^A-Za-z0-9_-]", "").toUpperCase();
        if (clean.length() > 40) clean = clean.substring(0, 40);
        String candidate = clean;
        int counter = 1;
        while (foodComboRepository.existsBySkuIgnoreCase(candidate)) {
            candidate = clean + "-" + counter;
            counter++;
        }
        return candidate;
    }

    private FoodItemStatus normalizeStatus(FoodItemStatus requestedStatus, FoodItemStatus fallbackStatus) {
        if (requestedStatus == null) {
            return fallbackStatus != null ? fallbackStatus : FoodItemStatus.ACTIVE;
        }
        return requestedStatus;
    }

    private Sort resolveSort(String sort) {
        if (sort == null) return Sort.by("name").ascending();
        return switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "name_desc" -> Sort.by("name").descending();
            default -> Sort.by("name").ascending();
        };
    }

    private PageRequest pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, Sort.by("name").ascending());
    }

    private FoodPriceHistoryResponse toPriceHistoryResponse(FoodPriceHistory h) {
        BigDecimal diff = h.getNewPrice().subtract(h.getOldPrice());
        return new FoodPriceHistoryResponse(
                h.getId(),
                h.getFoodItem() != null ? h.getFoodItem().getId() : null,
                h.getFoodCombo() != null ? h.getFoodCombo().getId() : null,
                h.getOldPrice(),
                h.getNewPrice(),
                diff,
                h.getChangedBy(),
                h.getChangedAt()
        );
    }
}
