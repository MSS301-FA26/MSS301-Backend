package com.cinemaai.payment.mapper;

import com.cinemaai.payment.dto.response.LoyaltyConfigurationResponse;
import com.cinemaai.payment.dto.response.LoyaltyReportResponse;
import com.cinemaai.payment.dto.response.LoyaltyResponse;
import com.cinemaai.payment.dto.response.LoyaltyTransactionResponse;
import com.cinemaai.payment.entity.LoyaltyConfiguration;
import com.cinemaai.payment.entity.LoyaltyPoint;
import com.cinemaai.payment.entity.LoyaltyPointTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class LoyaltyMapper {

    @Mapping(target = "userEmail", source = "userEmail")
    public abstract LoyaltyResponse toResponse(LoyaltyPoint point, String userEmail);

    public LoyaltyConfigurationResponse toConfigurationResponse(
            LoyaltyConfiguration cfg,
            Long requestedCinemaId,
            LocalDateTime defaultResetAt,
            String defaultResetSource
    ) {
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
                    defaultResetAt != null ? defaultResetAt.toLocalDate() : null,
                    defaultResetAt,
                    defaultResetSource
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
                cfg.getLastResetAt() != null ? cfg.getLastResetAt() : defaultResetAt,
                cfg.getLastResetSource() != null ? cfg.getLastResetSource() : defaultResetSource
        );
    }

    @Mapping(target = "pointTransactionId", source = "tx.id")
    @Mapping(target = "userId", source = "tx.userId")
    @Mapping(target = "customerName", source = "customerName")
    @Mapping(target = "customerPhone", source = "customerPhone")
    @Mapping(target = "customerEmail", source = "customerEmail")
    @Mapping(target = "bookingId", source = "tx.bookingId")
    @Mapping(target = "bookingCode", source = "tx.bookingCode")
    @Mapping(target = "movieTitle", source = "movieTitle")
    @Mapping(target = "showtimeStart", source = "showtimeStart")
    @Mapping(target = "roomName", source = "roomName")
    @Mapping(target = "type", expression = "java(tx.getType() != null ? tx.getType().name() : null)")
    @Mapping(target = "pointsDelta", source = "tx.pointsDelta")
    @Mapping(target = "balanceAfter", source = "tx.balanceAfter")
    @Mapping(target = "occurredAt", source = "tx.occurredAt")
    @Mapping(target = "note", source = "tx.note")
    public abstract LoyaltyTransactionResponse toTransactionResponse(
            LoyaltyPointTransaction tx,
            String customerName,
            String customerPhone,
            String customerEmail,
            String movieTitle,
            LocalDateTime showtimeStart,
            String roomName
    );

    public abstract LoyaltyReportResponse toReportResponse(
            LocalDateTime from,
            LocalDateTime to,
            long newMembers,
            long totalIssuedPoints,
            long totalBurnedPoints,
            double pointFlowRatio
    );
}
