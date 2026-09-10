package com.sba301.cinemaai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "ticket_combos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketCombo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, unique = true)
    private String name;

    @Setter
    @Column(length = 500)
    private String description;

    @Setter
    @Column(name = "adult_count", nullable = false)
    private int adultCount;

    @Setter
    @Column(name = "child_count", nullable = false)
    private int childCount;

    @Setter
    @Column(name = "student_count", nullable = false)
    private int studentCount;

    @Setter
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Setter
    @Column(nullable = false)
    private boolean active = true;

    public TicketCombo(
            String name,
            String description,
            int adultCount,
            int childCount,
            int studentCount,
            BigDecimal price
    ) {
        this.name = name;
        this.description = description;
        this.adultCount = adultCount;
        this.childCount = childCount;
        this.studentCount = studentCount;
        this.price = price;
    }
}
