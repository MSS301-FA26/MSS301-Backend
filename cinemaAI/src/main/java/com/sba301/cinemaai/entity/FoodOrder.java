package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.FoodOrderStatus;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Đơn bắp nước đặt sau khi booking đã thanh toán (add-on order).
 * <p>
 * Khác với {@code BookingFoodItem} (chọn đồ ăn lúc đặt vé), FoodOrder
 * đại diện cho một lần mua tại quầy (staff) hoặc online trong khi chưa hết suất chiếu.
 * Mỗi đơn có bản ghi {@code Payment} riêng, cho phép tách doanh thu bắp nước
 * khỏi doanh thu vé trong báo cáo.
 * </p>
 */
@Getter
@Entity
@Table(
        name = "food_orders",
        indexes = {
                @Index(name = "idx_food_orders_booking", columnList = "booking_id"),
                @Index(name = "idx_food_orders_customer", columnList = "customer_id"),
                @Index(name = "idx_food_orders_pending_expiry", columnList = "status,expires_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    @Column(name = "order_code", nullable = false, unique = true, length = 40)
    private String orderCode;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FoodOrderStatus status = FoodOrderStatus.PENDING_PAYMENT;

    @Setter
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Setter
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Setter
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Setter
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Setter
    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "created_by_staff", nullable = false)
    private boolean createdByStaff;

    public FoodOrder(Booking booking, User customer, String orderCode, boolean createdByStaff) {
        this.booking = booking;
        this.customer = customer;
        this.orderCode = orderCode;
        this.createdByStaff = createdByStaff;
    }
}
