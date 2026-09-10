package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.entity.WithdrawalRequest;
import com.sba301.cinemaai.enums.WithdrawalStatus;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    Page<WithdrawalRequest> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<WithdrawalRequest> findByStatusOrderByCreatedAtDesc(WithdrawalStatus status, Pageable pageable);

    Page<WithdrawalRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<WithdrawalRequest> findByIdAndUser(Long id, User user);

    long countByStatus(WithdrawalStatus status);

    @Query("select coalesce(sum(request.amount), 0) from WithdrawalRequest request where request.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") WithdrawalStatus status);
}
