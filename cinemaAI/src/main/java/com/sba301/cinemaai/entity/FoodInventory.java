package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.FoodStockStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(
        name = "food_inventories",
        uniqueConstraints = @UniqueConstraint(name = "uk_cinema_food_inventory", columnNames = {"cinema_id", "food_item_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodInventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id", nullable = false)
    private FoodItem foodItem;

    @Setter
    @Column(nullable = false)
    private int quantity = 0;

    @Setter
    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity = 0;

    @Setter
    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 10;

    public FoodInventory(Cinema cinema, FoodItem foodItem, int quantity, int lowStockThreshold) {
        this.cinema = cinema;
        this.foodItem = foodItem;
        this.quantity = Math.max(quantity, 0);
        this.reservedQuantity = 0;
        this.lowStockThreshold = Math.max(lowStockThreshold, 0);
    }

    public int getAvailableQuantity() {
        return Math.max(0, quantity - reservedQuantity);
    }

    public FoodStockStatus calculateStockStatus() {
        int available = getAvailableQuantity();
        if (available <= 0) {
            return FoodStockStatus.OUT_OF_STOCK;
        }
        if (available <= lowStockThreshold) {
            return FoodStockStatus.LOW_STOCK;
        }
        return FoodStockStatus.IN_STOCK;
    }
}
