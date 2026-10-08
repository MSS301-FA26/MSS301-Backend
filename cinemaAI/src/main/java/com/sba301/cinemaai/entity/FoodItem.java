package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.FoodItemStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "food_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, unique = true, length = 60)
    private String sku;

    @Setter
    @Column(nullable = false)
    private String name;

    @Setter
    @Column(length = 500)
    private String description;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private FoodCategory category;

    @Setter
    @Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Setter
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Setter
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FoodItemStatus status = FoodItemStatus.ACTIVE;

    @Setter
    @Column(name = "stock_tracking", nullable = false)
    private boolean stockTracking = true;

    @Setter
    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 10;

    @Setter
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Setter
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Setter
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    public FoodItem(String name, String description, BigDecimal price) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.costPrice = BigDecimal.ZERO;
        this.stockTracking = true;
        this.lowStockThreshold = 10;
        this.status = FoodItemStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
