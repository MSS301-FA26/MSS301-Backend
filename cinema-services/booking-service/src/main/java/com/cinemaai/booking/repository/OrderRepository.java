package com.cinemaai.booking.repository;

import com.cinemaai.booking.entity.Order;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("""
            select o from Order o where (:cinemaId is null or o.cinemaId = :cinemaId)
            and coalesce(o.paidAt, o.createdAt) >= :from
            and coalesce(o.paidAt, o.createdAt) < :to
            """)
    List<Order> findForAnalytics(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                 @Param("cinemaId") Long cinemaId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    List<Order> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
    Optional<Order> findByFoodOrderCode(String code);
}
