package com.cinemaai.payment.service.impl;

import com.cinemaai.payment.dto.response.LoyaltyConfigurationResponse;
import com.cinemaai.payment.dto.response.LoyaltyResponse;
import com.cinemaai.payment.entity.LoyaltyPoint;
import com.cinemaai.payment.enums.LoyaltyStatus;
import com.cinemaai.payment.exception.BadRequestException;
import com.cinemaai.payment.exception.NotFoundException;
import com.cinemaai.payment.repository.LoyaltyPointRepository;
import com.cinemaai.payment.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyServiceImpl implements LoyaltyService {

    private final LoyaltyPointRepository loyaltyPointRepository;

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

    @Override
    public LoyaltyConfigurationResponse getConfiguration() {
        return new LoyaltyConfigurationResponse(
                1L,
                BigDecimal.valueOf(10.0),
                1000,
                BigDecimal.valueOf(1000),
                12,
                31,
                "23:59:59",
                null,
                null,
                null
        );
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

        return LoyaltyResponse.builder()
                .userId(point.getUserId())
                .userEmail(point.getUserEmail())
                .points(point.getPoints())
                .totalPoints(point.getTotalPoints())
                .status(point.getStatus())
                .build();
    }

    @Override
    public LoyaltyConfigurationResponse updateConfiguration(com.cinemaai.payment.dto.request.LoyaltyConfigurationRequest request) {
        log.info("Admin updated loyalty config: earningRate={}, redemptionPoints={}, redemptionValue={}",
                request.earningRatePercent(), request.redemptionPoints(), request.redemptionValueVnd());
        return new LoyaltyConfigurationResponse(
                1L,
                request.earningRatePercent(),
                request.redemptionPoints(),
                request.redemptionValueVnd(),
                request.expiryMonth(),
                request.expiryDay(),
                request.expiryTime() != null ? request.expiryTime() : "23:59:59",
                null,
                java.time.LocalDateTime.now(),
                "ADMIN"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public com.cinemaai.payment.dto.response.PageResponse<com.cinemaai.payment.dto.response.LoyaltyTransactionResponse> searchTransactions(
            String keyword, java.time.LocalDateTime from, java.time.LocalDateTime to, int page, int size) {
        return new com.cinemaai.payment.dto.response.PageResponse<>(
                java.util.List.of(),
                java.util.List.of(),
                page,
                size,
                0L,
                0L,
                1,
                true,
                true
        );
    }

    @Override
    @Transactional(readOnly = true)
    public com.cinemaai.payment.dto.response.LoyaltyReportResponse getReport(
            java.time.LocalDateTime from, java.time.LocalDateTime to) {
        long totalPointsIssued = loyaltyPointRepository.findAll().stream()
                .mapToLong(LoyaltyPoint::getTotalPoints).sum();
        long activeMembers = loyaltyPointRepository.count();
        long currentPoints = loyaltyPointRepository.findAll().stream()
                .mapToLong(LoyaltyPoint::getPoints).sum();
        long burned = Math.max(0, totalPointsIssued - currentPoints);
        double flowRatio = totalPointsIssued > 0 ? (double) burned / totalPointsIssued : 0.0;

        return new com.cinemaai.payment.dto.response.LoyaltyReportResponse(
                from != null ? from : java.time.LocalDateTime.now().minusMonths(1),
                to != null ? to : java.time.LocalDateTime.now(),
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
                p.setPoints(0);
                loyaltyPointRepository.save(p);
                affected++;
            }
        }
        log.info("Expired loyalty points for {} accounts by source: {}", affected, source);
        return affected;
    }

    @Override
    @Transactional
    public LoyaltyResponse addPoints(com.cinemaai.payment.dto.request.LoyaltyAddRequest request) {
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
}
