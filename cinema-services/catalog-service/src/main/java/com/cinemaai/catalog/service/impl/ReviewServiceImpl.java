package com.cinemaai.catalog.service.impl;

import com.cinemaai.catalog.client.AuditClient;
import com.cinemaai.catalog.client.BookingClient;
import com.cinemaai.catalog.dto.request.review.CreateReviewRequest;
import com.cinemaai.catalog.dto.request.review.ReportReviewRequest;
import com.cinemaai.catalog.dto.request.review.UpdateReviewRequest;
import com.cinemaai.catalog.dto.response.AdminReviewStatsResponse;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.ReviewEligibilityResponse;
import com.cinemaai.catalog.dto.response.ReviewReportResponse;
import com.cinemaai.catalog.dto.response.ReviewResponse;
import com.cinemaai.catalog.dto.response.ReviewSummaryResponse;
import com.cinemaai.catalog.entity.Movie;
import com.cinemaai.catalog.entity.Review;
import com.cinemaai.catalog.entity.ReviewReport;
import com.cinemaai.catalog.enums.ReviewReportStatus;
import com.cinemaai.catalog.enums.ReviewStatus;
import com.cinemaai.catalog.exception.BadRequestException;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.repository.MovieRepository;
import com.cinemaai.catalog.repository.ReviewReportRepository;
import com.cinemaai.catalog.repository.ReviewRepository;
import com.cinemaai.catalog.service.ReviewService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final MovieRepository movieRepository;
    private final BookingClient bookingClient;
    private final AuditClient auditClient;

    private static final List<ReviewStatus> PUBLIC_STATUSES = List.of(ReviewStatus.PUBLISHED, ReviewStatus.VISIBLE);
    private static final int REPORT_FLAG_THRESHOLD = 3;

    @Override
    @Transactional(readOnly = true)
    public ReviewEligibilityResponse checkEligibility(Long userId, Long movieId) {
        resolveMovie(movieId);

        // Check if user already submitted a review that is not rejected or deleted
        Optional<Review> existing = reviewRepository.findActiveByUserAndMovie(
                userId, movieId, List.of(ReviewStatus.REJECTED, ReviewStatus.DELETED));

        if (existing.isPresent()) {
            Review rev = existing.get();
            return new ReviewEligibilityResponse(
                    false,
                    true,
                    "ALREADY_REVIEWED",
                    "Bạn đã gửi đánh giá cho phim này. Bạn có thể chỉnh sửa nhận xét của mình.",
                    rev.getBookingId(),
                    null,
                    null,
                    null,
                    ReviewResponse.from(rev)
            );
        }

        // Verify booking from booking-service
        Optional<BookingClient.BookingEligibilityDto> bookingOpt = bookingClient.checkBookingEligibility(userId, movieId);
        if (bookingOpt.isPresent()) {
            BookingClient.BookingEligibilityDto dto = bookingOpt.get();
            if (dto.hasWatched()) {
                return new ReviewEligibilityResponse(
                        true,
                        false,
                        null,
                        "Bạn đủ điều kiện viết đánh giá xác minh vé!",
                        dto.bookingId(),
                        dto.bookingCode(),
                        dto.showtimeStart(),
                        dto.cinemaName(),
                        null
                );
            }
            if (dto.bookingId() != null) {
                return new ReviewEligibilityResponse(
                        false,
                        false,
                        "SHOWTIME_NOT_FINISHED",
                        "Suất chiếu của bạn chưa kết thúc. Bạn chỉ có thể đánh giá sau khi đã xem phim.",
                        dto.bookingId(),
                        dto.bookingCode(),
                        dto.showtimeStart(),
                        dto.cinemaName(),
                        null
                );
            }
        }

        return new ReviewEligibilityResponse(
                false,
                false,
                "NO_VALID_BOOKING",
                "Bạn cần đặt vé và xem phim trước khi có thể viết đánh giá.",
                null,
                null,
                null,
                null,
                null
        );
    }

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, String email, String fullName, Long movieId, CreateReviewRequest request) {
        Movie movie = resolveMovie(movieId);

        // Rule 11 & 12: Verify booking eligibility
        ReviewEligibilityResponse eligibility = checkEligibility(userId, movieId);
        if (!eligibility.eligible()) {
            throw new BadRequestException(eligibility.messageVi() != null ? eligibility.messageVi() : "Không đủ điều kiện đánh giá phim.");
        }

        // Rule 13: 1 Customer + 1 Movie = 1 Review
        if (reviewRepository.findActiveByUserAndMovie(userId, movieId, List.of(ReviewStatus.REJECTED, ReviewStatus.DELETED)).isPresent()) {
            throw new BadRequestException("Bạn đã đánh giá phim này rồi. Vui lòng chọn sửa đánh giá.");
        }

        String displayName = (fullName != null && !fullName.isBlank()) ? fullName.trim() :
                (email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : "Khán giả");

        Review review = Review.builder()
                .movie(movie)
                .userId(userId)
                .userEmail(email)
                .userFullName(displayName)
                .bookingId(eligibility.bookingId())
                .rating(request.rating())
                .content(request.content().trim())
                .containsSpoiler(request.containsSpoiler())
                .verifiedBooking(true)
                .status(ReviewStatus.PUBLISHED)
                .reportCount(0)
                .build();

        Review saved = reviewRepository.save(review);
        log.info("User {} created verified review #{} for movie {}", email, saved.getId(), movieId);
        return ReviewResponse.from(saved);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, UpdateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đánh giá #" + reviewId));

        if (!review.getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền chỉnh sửa đánh giá này.");
        }

        if (!review.getStatus().isPubliclyVisible()) {
            throw new BadRequestException("Đánh giá đã bị ẩn hoặc kiểm duyệt, không thể chỉnh sửa.");
        }

        // Rule 35: 24h edit window
        if (review.getCreatedAt() != null && review.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new BadRequestException("Đánh giá chỉ có thể chỉnh sửa trong vòng 24 giờ kể từ khi đăng.");
        }

        review.setRating(request.rating());
        review.setContent(request.content().trim());
        review.setContainsSpoiler(request.containsSpoiler());

        Review updated = reviewRepository.save(review);
        log.info("User {} updated review #{}", userId, reviewId);
        return ReviewResponse.from(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getPublicReviewsByMovie(Long movieId, int page, int size) {
        resolveMovie(movieId);
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.min(Math.max(1, size), 50);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Review> reviewPage = reviewRepository.findByMovieIdAndStatusIn(movieId, PUBLIC_STATUSES, pageable);
        return PageResponse.from(reviewPage.map(ReviewResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummaryByMovie(Long movieId) {
        resolveMovie(movieId);

        long totalReviews = reviewRepository.countByMovieIdAndStatusIn(movieId, PUBLIC_STATUSES);
        long verifiedCount = reviewRepository.countByMovieIdAndVerifiedBookingTrueAndStatusIn(movieId, PUBLIC_STATUSES);

        double rawAvg = totalReviews > 0 ? reviewRepository.getAverageRatingByMovie(movieId, PUBLIC_STATUSES) : 0.0;
        double avg = BigDecimal.valueOf(rawAvg).setScale(1, RoundingMode.HALF_UP).doubleValue();
        double verifiedRatio = totalReviews > 0 ?
                BigDecimal.valueOf(((double) verifiedCount / totalReviews) * 100.0).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0.0;

        // Rating distribution map
        Map<String, ReviewSummaryResponse.RatingDistributionItem> distribution = new LinkedHashMap<>();
        List<Object[]> ratingCounts = reviewRepository.getRatingCountsByMovie(movieId, PUBLIC_STATUSES);
        Map<Integer, Long> scoreCountMap = new HashMap<>();
        for (Object[] row : ratingCounts) {
            Number ratingNum = (Number) row[0];
            Number countNum = (Number) row[1];
            if (ratingNum != null && countNum != null) {
                scoreCountMap.put(ratingNum.intValue(), countNum.longValue());
            }
        }

        // 5-Star buckets (5: 9-10, 4: 7-8, 3: 5-6, 2: 3-4, 1: 1-2)
        long count5 = scoreCountMap.getOrDefault(9, 0L) + scoreCountMap.getOrDefault(10, 0L);
        long count4 = scoreCountMap.getOrDefault(7, 0L) + scoreCountMap.getOrDefault(8, 0L);
        long count3 = scoreCountMap.getOrDefault(5, 0L) + scoreCountMap.getOrDefault(6, 0L);
        long count2 = scoreCountMap.getOrDefault(3, 0L) + scoreCountMap.getOrDefault(4, 0L);
        long count1 = scoreCountMap.getOrDefault(1, 0L) + scoreCountMap.getOrDefault(2, 0L);
        long count1_4 = count2 + count1;

        distribution.put("5", new ReviewSummaryResponse.RatingDistributionItem(count5, calcPercent(count5, totalReviews)));
        distribution.put("4", new ReviewSummaryResponse.RatingDistributionItem(count4, calcPercent(count4, totalReviews)));
        distribution.put("3", new ReviewSummaryResponse.RatingDistributionItem(count3, calcPercent(count3, totalReviews)));
        distribution.put("2", new ReviewSummaryResponse.RatingDistributionItem(count2, calcPercent(count2, totalReviews)));
        distribution.put("1", new ReviewSummaryResponse.RatingDistributionItem(count1, calcPercent(count1, totalReviews)));

        // Bracket buckets & legacy keys
        distribution.put("9_10", distribution.get("5"));
        distribution.put("7_8", distribution.get("4"));
        distribution.put("5_6", distribution.get("3"));
        distribution.put("3_4", distribution.get("2"));
        distribution.put("1_2", distribution.get("1"));
        distribution.put("1_4", new ReviewSummaryResponse.RatingDistributionItem(count1_4, calcPercent(count1_4, totalReviews)));

        return new ReviewSummaryResponse(movieId, avg, totalReviews, verifiedCount, verifiedRatio, distribution);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRating(Long movieId) {
        resolveMovie(movieId);
        long total = reviewRepository.countByMovieIdAndStatusIn(movieId, PUBLIC_STATUSES);
        if (total == 0) return 0.0;
        double rawAvg = reviewRepository.getAverageRatingByMovie(movieId, PUBLIC_STATUSES);
        return BigDecimal.valueOf(rawAvg).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void reportReview(Long userId, String userEmail, Long reviewId, ReportReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đánh giá #" + reviewId));

        if (review.getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không thể tự báo cáo đánh giá của chính mình.");
        }

        if (reviewReportRepository.existsByReporterUserIdAndReviewId(userId, reviewId)) {
            throw new BadRequestException("Bạn đã gửi báo cáo cho đánh giá này rồi.");
        }

        ReviewReport report = ReviewReport.builder()
                .review(review)
                .reporterUserId(userId)
                .reporterEmail(userEmail)
                .reason(request.reason())
                .description(request.description() != null ? request.description().trim() : null)
                .status(ReviewReportStatus.PENDING)
                .build();
        reviewReportRepository.save(report);

        // Increment report count & check flag threshold
        review.setReportCount(review.getReportCount() + 1);
        if (review.getReportCount() >= REPORT_FLAG_THRESHOLD && review.getStatus() == ReviewStatus.PUBLISHED) {
            review.setStatus(ReviewStatus.FLAGGED);
            log.info("Review #{} flagged automatically after reaching {} reports", reviewId, review.getReportCount());
        }
        reviewRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAdminReviews(
            Long movieId,
            String status,
            Integer rating,
            Boolean verified,
            String search,
            String sort,
            int page,
            int size
    ) {
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.min(Math.max(1, size), 100);

        Sort sortSpec;
        String s = sort != null ? sort.toUpperCase() : "NEWEST";
        switch (s) {
            case "OLDEST" -> sortSpec = Sort.by(Sort.Direction.ASC, "createdAt");
            case "RATING_HIGH" -> sortSpec = Sort.by(Sort.Direction.DESC, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "RATING_LOW" -> sortSpec = Sort.by(Sort.Direction.ASC, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "MOST_REPORTED" -> sortSpec = Sort.by(Sort.Direction.DESC, "reportCount").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            default -> sortSpec = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        Pageable pageable = PageRequest.of(boundedPage, boundedSize, sortSpec);

        Specification<Review> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), ReviewStatus.DELETED));

            if (movieId != null) {
                predicates.add(cb.equal(root.get("movie").get("id"), movieId));
            }

            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
                try {
                    ReviewStatus st = ReviewStatus.valueOf(status.toUpperCase());
                    predicates.add(cb.equal(root.get("status"), st));
                } catch (IllegalArgumentException ignored) {}
            }

            if (rating != null && rating >= 1 && rating <= 10) {
                predicates.add(cb.equal(root.get("rating"), rating));
            }

            if (verified != null) {
                predicates.add(cb.equal(root.get("verifiedBooking"), verified));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("userFullName")), pattern),
                        cb.like(cb.lower(root.get("userEmail")), pattern),
                        cb.like(cb.lower(root.get("movie").get("title")), pattern),
                        cb.like(cb.lower(root.get("content")), pattern)
                );
                predicates.add(searchPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Review> result = reviewRepository.findAll(spec, pageable);
        return PageResponse.from(result.map(ReviewResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getAdminReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đánh giá #" + reviewId));
        return ReviewResponse.from(review);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminReviewStatsResponse getAdminStats() {
        long totalReviews = reviewRepository.countByStatusIn(PUBLIC_STATUSES);
        double rawAvg = totalReviews > 0 ? reviewRepository.getOverallAverageRating(PUBLIC_STATUSES) : 0.0;
        double avgRating = BigDecimal.valueOf(rawAvg).setScale(1, RoundingMode.HALF_UP).doubleValue();
        long flaggedCount = reviewRepository.countByStatus(ReviewStatus.FLAGGED);
        long hiddenCount = reviewRepository.countByStatus(ReviewStatus.HIDDEN);
        long verifiedCount = reviewRepository.countByVerifiedBookingTrueAndStatusIn(PUBLIC_STATUSES);
        double verifiedRatio = totalReviews > 0 ?
                BigDecimal.valueOf(((double) verifiedCount / totalReviews) * 100.0).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0.0;

        return new AdminReviewStatsResponse(totalReviews, avgRating, flaggedCount, hiddenCount, verifiedRatio);
    }

    @Override
    @Transactional
    public ReviewResponse hideReview(Long reviewId, String reason, String adminEmail, Long adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đánh giá #" + reviewId));

        review.setStatus(ReviewStatus.HIDDEN);
        review.setHiddenAt(LocalDateTime.now());
        review.setHiddenBy(adminEmail);
        review.setModerationReason(reason != null && !reason.isBlank() ? reason.trim() : "Nội dung vi phạm tiêu chuẩn cộng đồng");

        Review saved = reviewRepository.save(review);
        log.info("Admin {} hid review #{} - Reason: {}", adminEmail, reviewId, review.getModerationReason());
        auditClient.record("UPDATE", "REVIEW", reviewId, "Ẩn đánh giá #" + reviewId + " - Lý do: " + review.getModerationReason(), adminId);
        return ReviewResponse.from(saved);
    }

    @Override
    @Transactional
    public ReviewResponse restoreReview(Long reviewId, String adminEmail, Long adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đánh giá #" + reviewId));

        review.setStatus(ReviewStatus.PUBLISHED);
        review.setHiddenAt(null);
        review.setHiddenBy(null);

        Review saved = reviewRepository.save(review);
        log.info("Admin {} restored review #{}", adminEmail, reviewId);
        auditClient.record("UPDATE", "REVIEW", reviewId, "Khôi phục hiển thị đánh giá #" + reviewId, adminId);
        return ReviewResponse.from(saved);
    }

    @Override
    @Transactional
    public ReviewResponse rejectReview(Long reviewId, String reason, String adminEmail, Long adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đánh giá #" + reviewId));

        review.setStatus(ReviewStatus.REJECTED);
        review.setHiddenAt(LocalDateTime.now());
        review.setHiddenBy(adminEmail);
        review.setModerationReason(reason != null && !reason.isBlank() ? reason.trim() : "Đánh giá bị từ chối do vi phạm quy định");

        Review saved = reviewRepository.save(review);
        log.info("Admin {} rejected review #{} - Reason: {}", adminEmail, reviewId, review.getModerationReason());
        auditClient.record("UPDATE", "REVIEW", reviewId, "Từ chối đánh giá #" + reviewId + " - Lý do: " + review.getModerationReason(), adminId);
        return ReviewResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewReportResponse> getReviewReports(Long reviewId) {
        return reviewReportRepository.findByReviewIdOrderByCreatedAtDesc(reviewId)
                .stream()
                .map(ReviewReportResponse::from)
                .toList();
    }

    private Movie resolveMovie(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phim với ID: " + movieId));
    }

    private double calcPercent(long count, long total) {
        if (total == 0) return 0.0;
        return BigDecimal.valueOf(((double) count / total) * 100.0).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
