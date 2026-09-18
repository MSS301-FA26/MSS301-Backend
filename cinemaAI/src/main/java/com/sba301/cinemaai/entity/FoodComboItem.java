package com.sba301.cinemaai.entity;

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
        name = "food_combo_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_combo_food_item", columnNames = {"combo_id", "food_item_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodComboItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id", nullable = false)
    private FoodCombo combo;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id", nullable = false)
    private FoodItem foodItem;

    @Setter
    @Column(nullable = false)
    private int quantity = 1;

    public FoodComboItem(FoodCombo combo, FoodItem foodItem, int quantity) {
        this.combo = combo;
        this.foodItem = foodItem;
        this.quantity = Math.max(quantity, 1);
    }
}
