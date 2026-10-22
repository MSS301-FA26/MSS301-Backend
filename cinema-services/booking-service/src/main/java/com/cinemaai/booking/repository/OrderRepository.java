package com.cinemaai.booking.repository;

import com.cinemaai.booking.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @org.springframework.data.jpa.repository.Query("""
            select o from Order o where (:cinemaId is null or o.cinemaId = :cinemaId)
            and coalesce(o.paidAt, o.createdAt) >= :from and coalesce(o.paidAt, o.createdAt) < :to
            """)
    List<Order> findForAnalytics(@org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
                                @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to,
                                @org.springframework.data.repository.query.Param("cinemaId") Long cinemaId);
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    List<Order> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
    Optional<Order> findByFoodOrderCode(String code);
}
