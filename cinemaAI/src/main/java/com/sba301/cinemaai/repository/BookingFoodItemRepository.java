package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.BookingFoodItem;
import com.sba301.cinemaai.entity.FoodOrder;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingFoodItemRepository extends JpaRepository<BookingFoodItem, Long> {

    List<BookingFoodItem> findByBooking(Booking booking);

    List<BookingFoodItem> findByFoodOrder(FoodOrder foodOrder);

    @EntityGraph(attributePaths = {"foodItem", "foodCombo", "foodOrder"})
    List<BookingFoodItem> findByBookingIn(Collection<Booking> bookings);

    // Món mua kèm vé tính theo ngày trả tiền VÉ; món mua THÊM (food order) tính theo
    // ngày thu tiền của chính đơn đó — không lệch kỳ báo cáo
    @org.springframework.data.jpa.repository.Query("""
            SELECT COALESCE(fi.name, fc.name),
                   SUM(f.quantity), SUM(f.quantity * f.unitPrice)
            FROM BookingFoodItem f
            LEFT JOIN f.booking b
            LEFT JOIN f.foodItem fi
            LEFT JOIN f.foodCombo fc
            LEFT JOIN f.foodOrder fo
            WHERE (
                (fo IS NULL AND b.status IN ('PAID','USED') AND b.paidAt >= :from AND b.paidAt < :to)
                OR (fo.status IN ('PAID', 'PICKED_UP') AND fo.paidAt >= :from AND fo.paidAt < :to)
              )
            GROUP BY COALESCE(fi.name, fc.name)
            ORDER BY SUM(f.quantity) DESC
            """)
    List<Object[]> concessionSales(
            @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);

    /**
     * One row per paid food transaction. Keeping the paid timestamp intact and grouping by
     * transaction in JPQL lets the service aggregate by LocalDate without database-specific
     * DATE()/date_trunc functions.
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT COALESCE(fo.paidAt, b.paidAt),
                   CASE
                       WHEN fo IS NULL THEN 'WITH_TICKET'
                       WHEN fo.booking IS NULL THEN 'STANDALONE'
                       ELSE 'ADD_ON'
                   END,
                   COALESCE(fo.id, b.id),
                   SUM(f.quantity),
                   SUM(f.quantity * f.unitPrice)
            FROM BookingFoodItem f
            LEFT JOIN f.booking b
            LEFT JOIN f.foodOrder fo
            WHERE (
                (fo IS NULL AND b.status IN ('PAID','USED') AND b.paidAt >= :from AND b.paidAt < :to)
                OR (fo.status IN ('PAID', 'PICKED_UP') AND fo.paidAt >= :from AND fo.paidAt < :to)
              )
            GROUP BY COALESCE(fo.paidAt, b.paidAt),
                     CASE
                         WHEN fo IS NULL THEN 'WITH_TICKET'
                         WHEN fo.booking IS NULL THEN 'STANDALONE'
                         ELSE 'ADD_ON'
                     END,
                     COALESCE(fo.id, b.id)
            ORDER BY COALESCE(fo.paidAt, b.paidAt)
            """)
    List<Object[]> concessionTransactions(
            @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);
}
