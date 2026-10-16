package com.cinemaai.payment.repository;

import com.cinemaai.payment.entity.RefundBatch;
import com.cinemaai.payment.enums.RefundBatchStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundBatchRepository extends JpaRepository<RefundBatch, Long> {
    Optional<RefundBatch> findByBatchCode(String batchCode);
    Optional<RefundBatch> findByShowtimeId(Long showtimeId);
    Page<RefundBatch> findByCinemaIdOrderByCreatedAtDesc(Long cinemaId, Pageable pageable);
    Page<RefundBatch> findByStatusOrderByCreatedAtDesc(RefundBatchStatus status, Pageable pageable);
}
