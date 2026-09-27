package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.food.StockAdjustmentRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.food.FoodInventoryResponse;
import com.sba301.cinemaai.dto.response.food.FoodInventoryTransactionResponse;
import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.entity.FoodComboItem;
import com.sba301.cinemaai.entity.FoodInventory;
import com.sba301.cinemaai.entity.FoodInventoryTransaction;
import com.sba301.cinemaai.entity.FoodItem;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.FoodInventoryTxType;
import com.sba301.cinemaai.enums.FoodStockStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.CinemaRepository;
import com.sba301.cinemaai.repository.FoodComboItemRepository;
import com.sba301.cinemaai.repository.FoodInventoryRepository;
import com.sba301.cinemaai.repository.FoodInventoryTransactionRepository;
import com.sba301.cinemaai.repository.FoodItemRepository;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.FoodInventoryService;
import java.util.List;
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
public class FoodInventoryServiceImpl implements FoodInventoryService {

    private final FoodInventoryRepository foodInventoryRepository;
    private final FoodInventoryTransactionRepository foodInventoryTransactionRepository;
    private final FoodItemRepository foodItemRepository;
    private final FoodComboItemRepository foodComboItemRepository;
    private final CinemaRepository cinemaRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<FoodInventoryResponse> getInventoriesByCinema(Long cinemaId) {
        return foodInventoryRepository.findByCinemaId(cinemaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodInventoryResponse> getInventoriesByFoodItem(Long foodItemId) {
        return foodInventoryRepository.findByFoodItemId(foodItemId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public FoodInventoryResponse adjustStock(StockAdjustmentRequest request, String currentUsername) {
        if (request.reason() == null || request.reason().trim().isEmpty()) {
            throw new BadRequestException("Lý do điều chỉnh tồn kho là bắt buộc.");
        }

        Cinema cinema = cinemaRepository.findById(request.cinemaId())
                .orElseThrow(() -> new NotFoundException("Cinema not found: " + request.cinemaId()));

        FoodItem foodItem = foodItemRepository.findById(request.foodItemId())
                .orElseThrow(() -> new NotFoundException("Food item not found: " + request.foodItemId()));

        FoodInventory inventory = foodInventoryRepository
                .findByCinemaIdAndFoodItemId(cinema.getId(), foodItem.getId())
                .orElseGet(() -> new FoodInventory(cinema, foodItem, 0, foodItem.getLowStockThreshold()));

        int beforeQuantity = inventory.getQuantity();
        int afterQuantity;

        if (request.newQuantity() != null) {
            afterQuantity = request.newQuantity();
        } else if (request.quantityDelta() != null) {
            afterQuantity = beforeQuantity + request.quantityDelta();
        } else {
            throw new BadRequestException("Either quantityDelta or newQuantity must be provided");
        }

        if (afterQuantity < 0) {
            throw new BadRequestException("Số lượng tồn kho không được âm. Tồn hiện tại: " + beforeQuantity + ", sau điều chỉnh: " + afterQuantity);
        }

        inventory.setQuantity(afterQuantity);
        FoodInventory saved = foodInventoryRepository.save(inventory);

        int delta = Math.abs(afterQuantity - beforeQuantity);
        FoodInventoryTransaction tx = new FoodInventoryTransaction(
                cinema,
                foodItem,
                request.type(),
                delta,
                beforeQuantity,
                afterQuantity,
                request.reason().trim(),
                null,
                currentUsername != null ? currentUsername : "ADMIN"
        );
        foodInventoryTransactionRepository.save(tx);

        auditLogService.record(
                AuditActionType.UPDATE,
                "FOOD_INVENTORY",
                saved.getId(),
                String.format("Adjusted %s at %s: %d -> %d (%s). Reason: %s",
                        foodItem.getName(), cinema.getName(), beforeQuantity, afterQuantity, request.type(), request.reason().trim())
        );

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FoodInventoryTransactionResponse> getTransactions(Long cinemaId, Long foodItemId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());

        Page<FoodInventoryTransaction> result;
        if (cinemaId != null) {
            result = foodInventoryTransactionRepository.findByCinemaIdOrderByCreatedAtDesc(cinemaId, pageable);
        } else {
            result = foodInventoryTransactionRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return PageResponse.from(result.map(this::toTxResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public int getAvailableStock(Long foodItemId, Long cinemaId) {
        if (cinemaId != null) {
            return foodInventoryRepository.findByCinemaIdAndFoodItemId(cinemaId, foodItemId)
                    .map(FoodInventory::getAvailableQuantity)
                    .orElse(0);
        }
        return foodInventoryRepository.getTotalAvailableStock(foodItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public FoodStockStatus calculateItemStockStatus(Long foodItemId, Long cinemaId) {
        FoodItem item = foodItemRepository.findById(foodItemId).orElse(null);
        if (item == null || !item.isStockTracking()) {
            return FoodStockStatus.IN_STOCK;
        }

        int available = getAvailableStock(foodItemId, cinemaId);
        int threshold = item.getLowStockThreshold();

        if (available <= 0) {
            return FoodStockStatus.OUT_OF_STOCK;
        }
        if (available <= threshold) {
            return FoodStockStatus.LOW_STOCK;
        }
        return FoodStockStatus.IN_STOCK;
    }

    @Override
    @Transactional(readOnly = true)
    public int calculateAvailableComboStock(Long comboId, Long cinemaId) {
        List<FoodComboItem> items = foodComboItemRepository.findByComboIdWithItems(comboId);
        if (items.isEmpty()) {
            return 0;
        }

        int minPossibleCombos = Integer.MAX_VALUE;
        for (FoodComboItem recipeItem : items) {
            FoodItem foodItem = recipeItem.getFoodItem();
            if (!foodItem.isStockTracking()) {
                continue;
            }

            int availableItemStock = getAvailableStock(foodItem.getId(), cinemaId);
            int requiredPerCombo = Math.max(recipeItem.getQuantity(), 1);
            int combosFromThisItem = availableItemStock / requiredPerCombo;

            if (combosFromThisItem < minPossibleCombos) {
                minPossibleCombos = combosFromThisItem;
            }
        }

        return minPossibleCombos == Integer.MAX_VALUE ? 999 : Math.max(0, minPossibleCombos);
    }

    @Override
    @Transactional(readOnly = true)
    public FoodStockStatus calculateComboStockStatus(Long comboId, Long cinemaId) {
        int maxCombos = calculateAvailableComboStock(comboId, cinemaId);
        if (maxCombos <= 0) {
            return FoodStockStatus.OUT_OF_STOCK;
        }
        if (maxCombos <= 5) {
            return FoodStockStatus.LOW_STOCK;
        }
        return FoodStockStatus.IN_STOCK;
    }

    @Override
    @Transactional
    public void deductStockForOrder(Long cinemaId, Long foodItemId, Long foodComboId, int quantity, String orderReference) {
        if (cinemaId == null || quantity <= 0) {
            return;
        }

        Cinema cinema = cinemaRepository.findById(cinemaId).orElse(null);
        if (cinema == null) return;

        if (foodItemId != null) {
            FoodItem item = foodItemRepository.findById(foodItemId).orElse(null);
            if (item != null && item.isStockTracking()) {
                deductSingleItemStock(cinema, item, quantity, orderReference);
            }
        } else if (foodComboId != null) {
            List<FoodComboItem> comboItems = foodComboItemRepository.findByComboIdWithItems(foodComboId);
            for (FoodComboItem comboItem : comboItems) {
                FoodItem item = comboItem.getFoodItem();
                if (item != null && item.isStockTracking()) {
                    int totalDeduct = comboItem.getQuantity() * quantity;
                    deductSingleItemStock(cinema, item, totalDeduct, orderReference + " (Combo)");
                }
            }
        }
    }

    private void deductSingleItemStock(Cinema cinema, FoodItem item, int deductQty, String reference) {
        FoodInventory inventory = foodInventoryRepository
                .findByCinemaIdAndFoodItemId(cinema.getId(), item.getId())
                .orElseGet(() -> new FoodInventory(cinema, item, 0, item.getLowStockThreshold()));

        int before = inventory.getQuantity();
        int after = Math.max(0, before - deductQty);
        inventory.setQuantity(after);
        foodInventoryRepository.save(inventory);

        FoodInventoryTransaction tx = new FoodInventoryTransaction(
                cinema,
                item,
                FoodInventoryTxType.SALE,
                deductQty,
                before,
                after,
                "Bán hàng: " + reference,
                reference,
                "ORDER_SYSTEM"
        );
        foodInventoryTransactionRepository.save(tx);
    }

    private FoodInventoryResponse toResponse(FoodInventory inv) {
        return new FoodInventoryResponse(
                inv.getId(),
                inv.getCinema().getId(),
                inv.getCinema().getName(),
                inv.getFoodItem().getId(),
                inv.getFoodItem().getName(),
                inv.getFoodItem().getSku(),
                inv.getQuantity(),
                inv.getReservedQuantity(),
                inv.getAvailableQuantity(),
                inv.getLowStockThreshold(),
                inv.calculateStockStatus(),
                inv.getUpdatedAt()
        );
    }

    private FoodInventoryTransactionResponse toTxResponse(FoodInventoryTransaction tx) {
        return new FoodInventoryTransactionResponse(
                tx.getId(),
                tx.getCinema().getId(),
                tx.getCinema().getName(),
                tx.getFoodItem().getId(),
                tx.getFoodItem().getName(),
                tx.getFoodItem().getSku(),
                tx.getType(),
                tx.getQuantity(),
                tx.getBeforeQuantity(),
                tx.getAfterQuantity(),
                tx.getReason(),
                tx.getReferenceId(),
                tx.getCreatedBy(),
                tx.getCreatedAt()
        );
    }
}
