package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    List<ReviewReport> findByReviewIdOrderByCreatedAtDesc(Long reviewId);

    boolean existsByReporterUserIdAndReviewId(Long reporterUserId, Long reviewId);

    long countByReviewId(Long reviewId);
}
