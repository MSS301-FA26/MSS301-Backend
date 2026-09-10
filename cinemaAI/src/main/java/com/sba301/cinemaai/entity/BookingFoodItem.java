package com.sba301.cinemaai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "booking_food_items",
        indexes = @Index(name = "idx_booking_food_items_booking", columnList = "booking_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingFoodItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id")
    private FoodItem foodItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_combo_id")
    private FoodCombo foodCombo;

    @lombok.Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_order_id")
    private FoodOrder foodOrder;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    public BookingFoodItem(Booking booking, FoodItem foodItem, FoodCombo foodCombo, int quantity, BigDecimal unitPrice) {
        this.booking = booking;
        this.foodItem = foodItem;
        this.foodCombo = foodCombo;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
}
