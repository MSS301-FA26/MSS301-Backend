package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.FoodInventoryTxType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(
        name = "food_inventory_transactions",
        indexes = {
                @Index(name = "idx_food_inv_tx_cinema_food", columnList = "cinema_id, food_item_id"),
                @Index(name = "idx_food_inv_tx_type", columnList = "type"),
                @Index(name = "idx_food_inv_tx_created", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodInventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id", nullable = false)
    private FoodItem foodItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FoodInventoryTxType type;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "before_quantity", nullable = false)
    private int beforeQuantity;

    @Column(name = "after_quantity", nullable = false)
    private int afterQuantity;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Setter
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public FoodInventoryTransaction(
            Cinema cinema,
            FoodItem foodItem,
            FoodInventoryTxType type,
            int quantity,
            int beforeQuantity,
            int afterQuantity,
            String reason,
            String referenceId,
            String createdBy
    ) {
        this.cinema = cinema;
        this.foodItem = foodItem;
        this.type = type;
        this.quantity = quantity;
        this.beforeQuantity = beforeQuantity;
        this.afterQuantity = afterQuantity;
        this.reason = reason;
        this.referenceId = referenceId;
        this.createdBy = createdBy;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
