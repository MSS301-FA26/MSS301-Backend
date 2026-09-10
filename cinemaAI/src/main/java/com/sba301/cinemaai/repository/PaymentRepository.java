package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.Payment;
import com.sba301.cinemaai.entity.FoodOrder;
import com.sba301.cinemaai.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBooking(Booking booking);

    Optional<Payment> findFirstByBookingOrderByIdDesc(Booking booking);

    List<Payment> findByFoodOrder(FoodOrder foodOrder);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS' AND p.paidAt BETWEEN :from AND :to")
    BigDecimal sumRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'SUCCESS' AND p.paidAt BETWEEN :from AND :to")
    long countSuccessfulPayments(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Phân rã doanh thu theo phương thức thanh toán (VNPay / Tiền mặt / Demo)
    @Query("""
            SELECT p.provider, SUM(p.amount), COUNT(p)
            FROM Payment p
            WHERE p.status = 'SUCCESS' AND p.paidAt BETWEEN :from AND :to
            GROUP BY p.provider
            ORDER BY SUM(p.amount) DESC
            """)
    List<Object[]> revenueByProvider(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Chỉ lấy payment của VÉ (không tính payment của food order đặt thêm) —
    // một booking có thể có nhiều payment SUCCESS sau khi mua thêm bắp nước.
    Optional<Payment> findFirstByBookingIdAndStatusAndFoodOrderIsNullOrderByIdDesc(Long bookingId, PaymentStatus status);

    // Bản batch cho danh sách booking: ORDER BY id DESC toàn cục nên payment đầu tiên
    // gặp được của mỗi booking chính là payment mới nhất của booking đó.
    List<Payment> findByBookingIdInAndStatusAndFoodOrderIsNullOrderByIdDesc(Collection<Long> bookingIds, PaymentStatus status);
}
