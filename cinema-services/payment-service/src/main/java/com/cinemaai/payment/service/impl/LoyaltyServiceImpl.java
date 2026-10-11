package com.cinemaai.payment.service.impl;

import com.cinemaai.payment.dto.request.AwardBookingPointsRequest;
import com.cinemaai.payment.dto.request.LoyaltyAddRequest;
import com.cinemaai.payment.dto.request.LoyaltyConfigurationRequest;
import com.cinemaai.payment.dto.request.RefundBookingPointsRequest;
import com.cinemaai.payment.dto.response.LoyaltyConfigurationResponse;
import com.cinemaai.payment.dto.response.LoyaltyReportResponse;
import com.cinemaai.payment.dto.response.LoyaltyResponse;
import com.cinemaai.payment.dto.response.LoyaltyTransactionResponse;
import com.cinemaai.payment.dto.response.PageResponse;
import com.cinemaai.payment.entity.LoyaltyPoint;
import com.cinemaai.payment.entity.LoyaltyPointTransaction;
import com.cinemaai.payment.enums.LoyaltyPointType;
import com.cinemaai.payment.enums.LoyaltyStatus;
import com.cinemaai.payment.exception.BadRequestException;
import com.cinemaai.payment.exception.NotFoundException;
import com.cinemaai.payment.repository.LoyaltyPointRepository;
import com.cinemaai.payment.repository.LoyaltyPointTransactionRepository;
import com.cinemaai.payment.service.LoyaltyService;
import com.cinemaai.payment.entity.LoyaltyConfiguration;
import com.cinemaai.payment.repository.LoyaltyConfigurationRepository;
import com.cinemaai.payment.client.IdentityClient;

import com.cinemaai.payment.exception.ForbiddenException;
import com.cinemaai.payment.security.AuthenticatedUser;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyServiceImpl implements LoyaltyService {

    private final LoyaltyPointRepository loyaltyPointRepository;
    private final LoyaltyPointTransactionRepository loyaltyPointTransactionRepository;
    private final LoyaltyConfigurationRepository loyaltyConfigurationRepository;
    private final IdentityClient identityClient;

    private final AtomicReference<BigDecimal> earningRatePercent = new AtomicReference<>(BigDecimal.valueOf(1.0));
    private final AtomicReference<Integer> redemptionPoints = new AtomicReference<>(1000);
    private final AtomicReference<BigDecimal> redemptionValueVnd = new AtomicReference<>(BigDecimal.valueOf(1000));
    private final AtomicReference<LocalDateTime> lastResetAt = new AtomicReference<>(null);
    private final AtomicReference<String> lastResetSource = new AtomicReference<>(null);

    @Override
    @Transactional
    public LoyaltyResponse getMyPoints(Long userId, String email) {
        if (userId == null) {
            return LoyaltyResponse.builder()
                    .userId(null)
                    .userEmail(email)
                    .points(0)
                    .totalPoints(0)
                    .status(LoyaltyStatus.ACTIVE)
                    .build();
        }

        LoyaltyPoint point = loyaltyPointRepository.findByUserId(userId)
                .orElseGet(() -> loyaltyPointRepository.save(LoyaltyPoint.builder()
                        .userId(userId)
                        .userEmail(email)
                        .points(0)
                        .totalPoints(0)
                        .status(LoyaltyStatus.ACTIVE)
                        .build()));

        return LoyaltyResponse.builder()
                .userId(point.getUserId())
                .userEmail(point.getUserEmail())
                .points(point.getPoints())
                .totalPoints(point.getTotalPoints())
                .status(point.getStatus())
                .build();
    }

    private LoyaltyConfigurationResponse mapToResponse(LoyaltyConfiguration cfg, Long requestedCinemaId) {
        if (cfg == null) {
            return new LoyaltyConfigurationResponse(
                    null,
                    requestedCinemaId,
                    null,
                    BigDecimal.valueOf(1.00),
                    BigDecimal.valueOf(100.00),
                    1000,
                    BigDecimal.valueOf(1000.00),
                    BigDecimal.valueOf(100.00),
                    12,
                    31,
                    "23:59:59",
                    lastResetAt.get() != null ? lastResetAt.get().toLocalDate() : null,
                    lastResetAt.get(),
                    lastResetSource.get()
            );
        }
        return new LoyaltyConfigurationResponse(
                cfg.getId(),
                cfg.getCinemaId() != null ? cfg.getCinemaId() : requestedCinemaId,
                cfg.getCinemaName(),
                cfg.getEarningRatePercent(),
                cfg.getRedemptionRatePercent(),
                cfg.getRedemptionPoints(),
                cfg.getRedemptionValueVnd(),
                cfg.getMaxRedemptionPercent(),
                cfg.getExpiryMonth(),
                cfg.getExpiryDay(),
                cfg.getExpiryTime(),
                cfg.getLastExpiredAt() != null ? cfg.getLastExpiredAt().toLocalDate() : null,
                cfg.getLastResetAt() != null ? cfg.getLastResetAt() : lastResetAt.get(),
                cfg.getLastResetSource() != null ? cfg.getLastResetSource() : lastResetSource.get()
        );
    }

    @Override
    public LoyaltyConfigurationResponse getConfiguration() {
        return getConfiguration(null);
    }

    @Override
    public LoyaltyConfigurationResponse getConfiguration(Long cinemaId) {
        if (cinemaId != null) {
            Optional<LoyaltyConfiguration> branchOpt = loyaltyConfigurationRepository.findByCinemaId(cinemaId);
            if (branchOpt.isPresent()) {
                return mapToResponse(branchOpt.get(), cinemaId);
            }
        }
        Optional<LoyaltyConfiguration> globalOpt = loyaltyConfigurationRepository.findByCinemaIdIsNull();
        if (globalOpt.isPresent()) {
            return mapToResponse(globalOpt.get(), cinemaId);
        }
        return mapToResponse(null, cinemaId);
    }

    @Override
    @Transactional
    public LoyaltyResponse redeemMyPoints(Long userId, String email, int points) {
        if (userId == null) {
            throw new BadRequestException("User must be authenticated to redeem points");
        }
        if (points <= 0) {
            throw new BadRequestException("Points to redeem must be greater than 0");
        }

        LoyaltyPoint point = loyaltyPointRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Loyalty balance not found"));

        if (point.getPoints() < points) {
            throw new BadRequestException("Insufficient loyalty points");
        }

        point.setPoints(point.getPoints() - points);
        loyaltyPointRepository.save(point);

        loyaltyPointTransactionRepository.save(LoyaltyPointTransaction.builder()
                .userId(userId)
                .type(LoyaltyPointType.REDEEM)
                .pointsDelta(-points)
                .balanceAfter(point.getPoints())
                .note("Khách hàng tự đổi " + points + " điểm thưởng")
                .occurredAt(LocalDateTime.now())
                .build());

        return LoyaltyResponse.builder()
                .userId(point.getUserId())
                .userEmail(point.getUserEmail())
                .points(point.getPoints())
                .totalPoints(point.getTotalPoints())
                .status(point.getStatus())
                .build();
    }

    @Override
    public LoyaltyConfigurationResponse updateConfiguration(LoyaltyConfigurationRequest request) {
        return updateConfiguration(request, null);
    }

    @Override
    @Transactional
    public LoyaltyConfigurationResponse updateConfiguration(LoyaltyConfigurationRequest request, AuthenticatedUser user) {
        if (request == null) {
            throw new BadRequestException("Cấu hình không hợp lệ");
        }
        Long targetCinemaId = request.cinemaId();

        // Kiểm tra phân quyền Manager: nếu là Manager thì bắt buộc phải có chi nhánh và chỉ được cấu hình chi nhánh của mình
        if (user != null) {
            boolean isAdmin = user.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isManager = user.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

            if (isManager && !isAdmin && targetCinemaId == null) {
                throw new ForbiddenException("Quản lý rạp chỉ có thể cấu hình điểm thưởng cho chi nhánh của mình");
            }
        }

        LoyaltyConfiguration cfg;
        if (targetCinemaId != null) {
            cfg = loyaltyConfigurationRepository.findByCinemaId(targetCinemaId)
                    .orElseGet(() -> LoyaltyConfiguration.builder()
                            .cinemaId(targetCinemaId)
                            .cinemaName(request.cinemaName())
                            .build());
        } else {
            cfg = loyaltyConfigurationRepository.findByCinemaIdIsNull()
                    .orElseGet(() -> LoyaltyConfiguration.builder()
                            .cinemaName("Toàn hệ thống (Mặc định)")
                            .build());
        }

        if (request.cinemaName() != null && !request.cinemaName().isBlank()) {
            cfg.setCinemaName(request.cinemaName());
        }
        if (request.earningRatePercent() != null) {
            cfg.setEarningRatePercent(request.earningRatePercent());
            if (targetCinemaId == null) {
                earningRatePercent.set(request.earningRatePercent());
            }
        }
        if (request.redemptionRatePercent() != null) {
            cfg.setRedemptionRatePercent(request.redemptionRatePercent());
        }
        if (request.redemptionPoints() > 0) {
            cfg.setRedemptionPoints(request.redemptionPoints());
            if (targetCinemaId == null) {
                redemptionPoints.set(request.redemptionPoints());
            }
        }
        if (request.redemptionValueVnd() != null) {
            cfg.setRedemptionValueVnd(request.redemptionValueVnd());
            if (targetCinemaId == null) {
                redemptionValueVnd.set(request.redemptionValueVnd());
            }
        }
        if (request.maxRedemptionPercent() != null) {
            cfg.setMaxRedemptionPercent(request.maxRedemptionPercent());
        }
        if (request.expiryMonth() >= 1 && request.expiryMonth() <= 12) {
            cfg.setExpiryMonth(request.expiryMonth());
        }
        if (request.expiryDay() >= 1 && request.expiryDay() <= 31) {
            cfg.setExpiryDay(request.expiryDay());
        }
        if (request.expiryTime() != null && !request.expiryTime().isBlank()) {
            cfg.setExpiryTime(request.expiryTime());
        }
        if (request.expiryDate() != null) {
            cfg.setExpiryDate(request.expiryDate());
        }

        LoyaltyConfiguration saved = loyaltyConfigurationRepository.save(cfg);

        // Khi cấu hình toàn hệ thống (targetCinemaId == null), đồng bộ cập nhật tất cả các chi nhánh rạp hiện có
        if (targetCinemaId == null) {
            List<LoyaltyConfiguration> branchConfigs = loyaltyConfigurationRepository.findAll();
            for (LoyaltyConfiguration branch : branchConfigs) {
                if (branch.getCinemaId() != null) {
                    if (request.earningRatePercent() != null) branch.setEarningRatePercent(request.earningRatePercent());
                    if (request.redemptionRatePercent() != null) branch.setRedemptionRatePercent(request.redemptionRatePercent());
                    if (request.redemptionPoints() > 0) branch.setRedemptionPoints(request.redemptionPoints());
                    if (request.redemptionValueVnd() != null) branch.setRedemptionValueVnd(request.redemptionValueVnd());
                    if (request.maxRedemptionPercent() != null) branch.setMaxRedemptionPercent(request.maxRedemptionPercent());
                    if (request.expiryMonth() >= 1 && request.expiryMonth() <= 12) branch.setExpiryMonth(request.expiryMonth());
                    if (request.expiryDay() >= 1 && request.expiryDay() <= 31) branch.setExpiryDay(request.expiryDay());
                    if (request.expiryTime() != null && !request.expiryTime().isBlank()) branch.setExpiryTime(request.expiryTime());
                    if (request.expiryDate() != null) branch.setExpiryDate(request.expiryDate());
                    loyaltyConfigurationRepository.save(branch);
                }
            }
            log.info("Synchronized global loyalty configuration to all existing cinema branches");
        }

        log.info("Updated loyalty config for cinemaId={}: earningRate={}, redemptionRate={}, redemptionPoints={}, redemptionValue={}",
                targetCinemaId, saved.getEarningRatePercent(), saved.getRedemptionRatePercent(), saved.getRedemptionPoints(), saved.getRedemptionValueVnd());

        return mapToResponse(saved, targetCinemaId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoyaltyTransactionResponse> searchTransactions(
            String keyword, LocalDateTime from, LocalDateTime to, int page, int size) {

        Specification<LoyaltyPointTransaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("bookingCode")), pattern),
                        cb.like(cb.lower(root.get("note")), pattern)
                ));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<LoyaltyPointTransaction> txPage = loyaltyPointTransactionRepository.findAll(spec, pageable);

        java.util.Map<Long, IdentityClient.UserProfileDto> userCache = new java.util.concurrent.ConcurrentHashMap<>();

        List<LoyaltyTransactionResponse> items = txPage.getContent().stream()
                .map(tx -> {
                    IdentityClient.UserProfileDto user = null;
                    if (tx.getUserId() != null && identityClient != null) {
                        user = userCache.computeIfAbsent(tx.getUserId(), id -> identityClient.getUserProfile(id));
                    }
                    String custName = user != null && user.fullName() != null && !user.fullName().isBlank() ? user.fullName() : null;
                    String custPhone = user != null ? user.phone() : null;
                    String custEmail = user != null ? user.email() : null;

                    return new LoyaltyTransactionResponse(
                            tx.getId(),
                            tx.getUserId(),
                            custName,
                            custPhone,
                            custEmail,
                            tx.getBookingId(),
                            tx.getBookingCode(),
                            null,
                            null,
                            null,
                            tx.getType().name(),
                            tx.getPointsDelta(),
                            tx.getBalanceAfter(),
                            tx.getOccurredAt(),
                            tx.getNote()
                    );
                })
                .toList();

        return new PageResponse<>(
                items,
                items,
                txPage.getNumber(),
                txPage.getSize(),
                txPage.getTotalElements(),
                txPage.getTotalElements(),
                txPage.getTotalPages(),
                txPage.isFirst(),
                txPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LoyaltyReportResponse getReport(LocalDateTime from, LocalDateTime to) {
        long totalPointsIssued = loyaltyPointRepository.findAll().stream()
                .mapToLong(LoyaltyPoint::getTotalPoints).sum();
        long activeMembers = loyaltyPointRepository.count();
        long currentPoints = loyaltyPointRepository.findAll().stream()
                .mapToLong(LoyaltyPoint::getPoints).sum();
        long burned = Math.max(0, totalPointsIssued - currentPoints);
        double flowRatio = totalPointsIssued > 0 ? (double) burned / totalPointsIssued : 0.0;

        return new LoyaltyReportResponse(
                from != null ? from : LocalDateTime.now().minusMonths(1),
                to != null ? to : LocalDateTime.now(),
                activeMembers,
                totalPointsIssued,
                burned,
                flowRatio
        );
    }

    @Override
    @Transactional
    public int expireAllActivePoints(String source) {
        var points = loyaltyPointRepository.findAll();
        int affected = 0;
        for (var p : points) {
            if (p.getPoints() > 0) {
                int lost = p.getPoints();
                p.setPoints(0);
                loyaltyPointRepository.save(p);
                affected++;

                loyaltyPointTransactionRepository.save(LoyaltyPointTransaction.builder()
                        .userId(p.getUserId())
                        .type(LoyaltyPointType.EXPIRE)
                        .pointsDelta(-lost)
                        .balanceAfter(0)
                        .note("Điểm thưởng hết hạn theo chính sách reset (" + source + ")")
                        .occurredAt(LocalDateTime.now())
                        .build());
            }
        }
        lastResetAt.set(LocalDateTime.now());
        lastResetSource.set(source);
        log.info("Expired loyalty points for {} accounts by source: {}", affected, source);
        return affected;
    }

    @Override
    @Transactional
    public LoyaltyResponse addPoints(LoyaltyAddRequest request) {
        LoyaltyPoint point = loyaltyPointRepository.findByUserId(request.getUserId())
                .orElseGet(() -> loyaltyPointRepository.save(LoyaltyPoint.builder()
                        .userId(request.getUserId())
                        .points(0)
                        .totalPoints(0)
                        .status(LoyaltyStatus.ACTIVE)
                        .build()));

        point.setPoints(point.getPoints() + request.getPoints());
        point.setTotalPoints(point.getTotalPoints() + request.getPoints());
        loyaltyPointRepository.save(point);

        loyaltyPointTransactionRepository.save(LoyaltyPointTransaction.builder()
                .userId(request.getUserId())
                .type(LoyaltyPointType.ADJUST)
                .pointsDelta(request.getPoints())
                .balanceAfter(point.getPoints())
                .note("Admin điều chỉnh cộng điểm: " + request.getReason())
                .occurredAt(LocalDateTime.now())
                .build());

        log.info("Admin granted {} points to user #{}. Reason: {}", request.getPoints(), request.getUserId(), request.getReason());
        return LoyaltyResponse.builder()
                .userId(point.getUserId())
                .userEmail(point.getUserEmail())
                .points(point.getPoints())
                .totalPoints(point.getTotalPoints())
                .status(point.getStatus())
                .build();
    }

    @Override
    @Transactional
    public LoyaltyResponse redeemPoints(Long userId, int points) {
        return redeemMyPoints(userId, null, points);
    }

    @Override
    @Transactional
    public LoyaltyResponse awardPointsForBooking(AwardBookingPointsRequest request) {
        if (request == null || request.userId() == null) {
            log.info("Skipping loyalty points award: no userId provided");
            return null;
        }

        Long userId = request.userId();
        Long bookingId = request.bookingId();
        String bookingCode = request.bookingCode() != null ? request.bookingCode() : ("#" + bookingId);
        BigDecimal amount = request.amount() != null ? request.amount() : BigDecimal.ZERO;
        Integer redeemedPoints = request.redeemedPoints() != null ? request.redeemedPoints() : 0;

        LoyaltyPoint lp = loyaltyPointRepository.findByUserId(userId)
                .orElseGet(() -> loyaltyPointRepository.save(LoyaltyPoint.builder()
                        .userId(userId)
                        .points(0)
                        .totalPoints(0)
                        .status(LoyaltyStatus.ACTIVE)
                        .build()));

        // 1. If customer redeemed points for this booking, record REDEEM if not already recorded
        if (redeemedPoints > 0 && bookingId != null) {
            long existingRedeem = loyaltyPointTransactionRepository.sumDeltaByBookingIdAndType(bookingId, LoyaltyPointType.REDEEM);
            if (existingRedeem == 0) {
                int currentPoints = lp.getPoints();
                int newPoints = Math.max(0, currentPoints - redeemedPoints);
                lp.setPoints(newPoints);
                loyaltyPointRepository.save(lp);

                loyaltyPointTransactionRepository.save(LoyaltyPointTransaction.builder()
                        .userId(userId)
                        .bookingId(bookingId)
                        .bookingCode(bookingCode)
                        .type(LoyaltyPointType.REDEEM)
                        .pointsDelta(-redeemedPoints)
                        .balanceAfter(lp.getPoints())
                        .note("Đổi " + redeemedPoints + " điểm giảm giá đơn đặt vé " + bookingCode)
                        .occurredAt(LocalDateTime.now())
                        .build());
                log.info("Deducted {} redeemed points from userId {} for booking {}", redeemedPoints, userId, bookingCode);
            }
        }

        // 2. Check if EARN points already awarded for this booking (Idempotency)
        if (bookingId != null) {
            long alreadyEarned = loyaltyPointTransactionRepository.sumDeltaByBookingIdAndType(bookingId, LoyaltyPointType.EARN);
            if (alreadyEarned > 0) {
                log.info("Loyalty points already earned for booking {}: {} points, skipping duplicate award", bookingCode, alreadyEarned);
                return LoyaltyResponse.builder()
                        .userId(lp.getUserId())
                        .userEmail(lp.getUserEmail())
                        .points(lp.getPoints())
                        .totalPoints(lp.getTotalPoints())
                        .status(lp.getStatus())
                        .build();
            }
        }

        // 3. Calculate points to earn (based on cinema-specific or global rate)
        BigDecimal rate = earningRatePercent.get();
        if (request.cinemaId() != null && request.cinemaId() > 0) {
            Optional<LoyaltyConfiguration> branchOpt = loyaltyConfigurationRepository.findByCinemaId(request.cinemaId());
            if (branchOpt.isPresent() && branchOpt.get().getEarningRatePercent() != null) {
                rate = branchOpt.get().getEarningRatePercent();
            }
        }

        int earned = amount.multiply(rate)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .intValue();

        if (earned > 0) {
            lp.setPoints(lp.getPoints() + earned);
            lp.setTotalPoints(lp.getTotalPoints() + earned);
            loyaltyPointRepository.save(lp);

            loyaltyPointTransactionRepository.save(LoyaltyPointTransaction.builder()
                    .userId(userId)
                    .bookingId(bookingId)
                    .bookingCode(bookingCode)
                    .type(LoyaltyPointType.EARN)
                    .pointsDelta(earned)
                    .balanceAfter(lp.getPoints())
                    .note("Tích điểm từ đơn đặt vé " + bookingCode + " (" + rate + "% trên " + amount + "đ)")
                    .occurredAt(LocalDateTime.now())
                    .build());
            log.info("Awarded {} loyalty points to userId {} for booking {} at rate {}%", earned, userId, bookingCode, rate);
        }

        return LoyaltyResponse.builder()
                .userId(lp.getUserId())
                .userEmail(lp.getUserEmail())
                .points(lp.getPoints())
                .totalPoints(lp.getTotalPoints())
                .status(lp.getStatus())
                .build();
    }

    @Override
    @Transactional
    public LoyaltyResponse refundPointsForBooking(RefundBookingPointsRequest request) {
        if (request == null || request.userId() == null) {
            log.info("Skipping loyalty points refund: no userId provided");
            return null;
        }

        Long userId = request.userId();
        Long bookingId = request.bookingId();
        String bookingCode = request.bookingCode() != null ? request.bookingCode() : ("#" + bookingId);
        BigDecimal amount = request.amount() != null ? request.amount() : BigDecimal.ZERO;
        Integer redeemedPoints = request.redeemedPoints() != null ? request.redeemedPoints() : 0;
        String reason = request.reason() != null && !request.reason().isBlank() ? request.reason() : "Hoàn vé / Hủy suất chiếu";

        LoyaltyPoint lp = loyaltyPointRepository.findByUserId(userId)
                .orElseGet(() -> loyaltyPointRepository.save(LoyaltyPoint.builder()
                        .userId(userId)
                        .points(0)
                        .totalPoints(0)
                        .status(LoyaltyStatus.ACTIVE)
                        .build()));

        // 1. Restore redeemed points if any, with idempotency check
        if (redeemedPoints > 0 && bookingId != null) {
            long alreadyRestored = loyaltyPointTransactionRepository.sumDeltaByBookingIdAndType(bookingId, LoyaltyPointType.RESTORE);
            if (alreadyRestored == 0) {
                lp.setPoints(lp.getPoints() + redeemedPoints);
                loyaltyPointRepository.save(lp);

                loyaltyPointTransactionRepository.save(LoyaltyPointTransaction.builder()
                        .userId(userId)
                        .bookingId(bookingId)
                        .bookingCode(bookingCode)
                        .type(LoyaltyPointType.RESTORE)
                        .pointsDelta(redeemedPoints)
                        .balanceAfter(lp.getPoints())
                        .note("Hoàn lại " + redeemedPoints + " điểm đã dùng từ vé bị hủy: " + bookingCode + " (" + reason + ")")
                        .occurredAt(LocalDateTime.now())
                        .build());
                log.info("Restored {} redeemed points to userId {} for refunded booking {}", redeemedPoints, userId, bookingCode);
            }
        }

        // 2. Revoke earned points with idempotency check
        if (bookingId != null) {
            long alreadyRevoked = Math.abs(loyaltyPointTransactionRepository.sumDeltaByBookingIdAndType(bookingId, LoyaltyPointType.REVOKE));
            if (alreadyRevoked == 0) {
                long previouslyEarned = loyaltyPointTransactionRepository.sumDeltaByBookingIdAndType(bookingId, LoyaltyPointType.EARN);
                int pointsToRevoke = (int) previouslyEarned;
                if (pointsToRevoke <= 0 && amount.compareTo(BigDecimal.ZERO) > 0) {
                    pointsToRevoke = amount.multiply(earningRatePercent.get())
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                            .intValue();
                }

                if (pointsToRevoke > 0) {
                    int newPoints = Math.max(0, lp.getPoints() - pointsToRevoke);
                    lp.setPoints(newPoints);
                    loyaltyPointRepository.save(lp);

                    loyaltyPointTransactionRepository.save(LoyaltyPointTransaction.builder()
                            .userId(userId)
                            .bookingId(bookingId)
                            .bookingCode(bookingCode)
                            .type(LoyaltyPointType.REVOKE)
                            .pointsDelta(-pointsToRevoke)
                            .balanceAfter(lp.getPoints())
                            .note("Thu hồi " + pointsToRevoke + " điểm đã tích từ vé được hoàn tiền: " + bookingCode + " (" + reason + ")")
                            .occurredAt(LocalDateTime.now())
                            .build());
                    log.info("Revoked {} earned points from userId {} for refunded booking {}", pointsToRevoke, userId, bookingCode);
                }
            }
        }

        return LoyaltyResponse.builder()
                .userId(lp.getUserId())
                .userEmail(lp.getUserEmail())
                .points(lp.getPoints())
                .totalPoints(lp.getTotalPoints())
                .status(lp.getStatus())
                .build();
    }

    @Override
    @Transactional
    public LoyaltyResponse awardPointsForFoodOrder(Long userId, Long foodOrderId, String orderCode, BigDecimal amount) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return null;
        int earned = amount.multiply(earningRatePercent.get())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .intValue();
        if (earned <= 0) return null;

        LoyaltyPoint lp = loyaltyPointRepository.findByUserId(userId)
                .orElseGet(() -> loyaltyPointRepository.save(LoyaltyPoint.builder()
                        .userId(userId)
                        .points(0)
                        .totalPoints(0)
                        .status(LoyaltyStatus.ACTIVE)
                        .build()));

        lp.setPoints(lp.getPoints() + earned);
        lp.setTotalPoints(lp.getTotalPoints() + earned);
        loyaltyPointRepository.save(lp);

        loyaltyPointTransactionRepository.save(LoyaltyPointTransaction.builder()
                .userId(userId)
                .bookingId(foodOrderId)
                .bookingCode(orderCode != null ? orderCode : ("FO-" + foodOrderId))
                .type(LoyaltyPointType.EARN)
                .pointsDelta(earned)
                .balanceAfter(lp.getPoints())
                .note("Tích điểm từ đơn bắp nước " + orderCode + " (1% trên " + amount + "đ)")
                .occurredAt(LocalDateTime.now())
                .build());

        log.info("Awarded {} loyalty points for food order {} to userId {}", earned, orderCode, userId);
        return LoyaltyResponse.builder()
                .userId(lp.getUserId())
                .userEmail(lp.getUserEmail())
                .points(lp.getPoints())
                .totalPoints(lp.getTotalPoints())
                .status(lp.getStatus())
                .build();
    }
}
