package com.sba301.cinemaai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "food_categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Setter
    @Column(nullable = false, length = 100)
    private String name;

    @Setter
    @Column(length = 500)
    private String description;

    @Setter
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Setter
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Setter
    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Setter
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public FoodCategory(String code, String name, String description, int sortOrder) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.status = "ACTIVE";
    }
}
