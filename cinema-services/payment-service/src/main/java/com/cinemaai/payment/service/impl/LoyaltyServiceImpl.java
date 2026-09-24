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
}
