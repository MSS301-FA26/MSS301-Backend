package com.cinemaai.booking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Entity
@Table(
        name = "food_orders",
        indexes = {
                @Index(name = "idx_food_orders_user", columnList = "user_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "food_order_code", nullable = false, unique = true, length = 50)
    private String foodOrderCode;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "PENDING_PAYMENT";

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Builder.Default
    @OneToMany(mappedBy = "foodOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FoodOrderItem> items = new ArrayList<>();
}
