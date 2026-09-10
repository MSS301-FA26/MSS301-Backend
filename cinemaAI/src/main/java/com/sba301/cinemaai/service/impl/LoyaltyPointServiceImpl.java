package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.loyalty.LoyaltyAddRequest;
import com.sba301.cinemaai.dto.request.loyalty.LoyaltyConfigurationRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.loyalty.LoyaltyConfigurationResponse;
import com.sba301.cinemaai.dto.response.loyalty.LoyaltyReportResponse;
import com.sba301.cinemaai.dto.response.loyalty.LoyaltyResponse;
import com.sba301.cinemaai.dto.response.loyalty.LoyaltyTransactionResponse;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.FoodOrder;
import com.sba301.cinemaai.entity.LoyaltyConfiguration;
import com.sba301.cinemaai.entity.LoyaltyPoint;
import com.sba301.cinemaai.entity.LoyaltyPointTransaction;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.LoyaltyPointType;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.LoyaltyConfigurationRepository;
import com.sba301.cinemaai.repository.LoyaltyPointRepository;
import com.sba301.cinemaai.repository.LoyaltyPointTransactionRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.LoyaltyPointService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyPointServiceImpl implements LoyaltyPointService {

    private static final long MIN_EXPIRY_LEAD_MINUTES = 15;

    private final LoyaltyPointRepository loyaltyPointRepository;
    private final LoyaltyConfigurationRepository loyaltyConfigurationRepository;
    private final LoyaltyPointTransactionRepository loyaltyPointTransactionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public LoyaltyResponse getMyPoints(String email) {
        User user = resolveUserByEmail(email);
        LoyaltyPoint lp = getOrCreate(user);
        return LoyaltyResponse.from(lp);
    }

    @Override
    @Transactional
    public LoyaltyResponse addPoints(LoyaltyAddRequest request) {
        User user = resolveUserById(request.getUserId());
        LoyaltyPoint lp = getOrCreate(user);
        addPoints(lp, request.getPoints());
        loyaltyPointRepository.save(lp);
        recordTransaction(user, null, LoyaltyPointType.ADJUST, request.getPoints(), lp.getPoints(), request.getReason());
        log.info("Added {} points to user {}", request.getPoints(), user.getEmail());
        auditLogService.record(AuditActionType.UPDATE, "LOYALTY", user.getId(),
                "User #" + user.getId() + " +" + request.getPoints() + " điểm: " + request.getReason());
        return LoyaltyResponse.from(lp);
    }

    @Override
    @Transactional
    public LoyaltyResponse redeemPoints(Long userId, int points) {
        if (points <= 0) {
            throw new BadRequestException("Points to redeem must be positive");
        }
        User user = resolveUserById(userId);
        LoyaltyPoint lp = getOrCreate(user);
        if (lp.getPoints() < points) {
            throw new BadRequestException("Insufficient points. Available: " + lp.getPoints() + ", requested: " + points);
        }
        redeemPoints(lp, points);
        loyaltyPointRepository.save(lp);
        recordTransaction(user, null, LoyaltyPointType.ADJUST, -points, lp.getPoints(), "Admin redeemed points");
        return LoyaltyResponse.from(lp);
    }

    @Override
    @Transactional
    public LoyaltyResponse redeemMyPoints(String email, int points) {
        return redeemPoints(resolveUserByEmail(email).getId(), points);
    }

    @Override
    @Transactional
    public void addPointsFromBooking(User user, Booking booking) {
        int earned = calculateEarnedPoints(booking);
        if (earned <= 0) return;
        LoyaltyPoint lp = getOrCreate(user);
        addPoints(lp, earned);
        loyaltyPointRepository.save(lp);
        recordTransaction(user, booking, LoyaltyPointType.EARN, earned, lp.getPoints(), "Earned from booking " + booking.getBookingCode());
    }

    @Override
    @Transactional
    public void addPointsFromFoodOrder(User user, FoodOrder foodOrder) {
        int earned = calculateEarnedPoints(foodOrder.getTotalAmount());
        if (earned <= 0) return;
        LoyaltyPoint loyaltyPoint = getOrCreate(user);
        addPoints(loyaltyPoint, earned);
        loyaltyPointRepository.save(loyaltyPoint);
        recordTransaction(
                user,
                foodOrder.getBooking(),
                LoyaltyPointType.EARN,
                earned,
                loyaltyPoint.getPoints(),
                "Earned from food order " + foodOrder.getOrderCode()
        );
    }

    @Override
    @Transactional
    public void revokePointsFromBooking(User user, Booking booking) {
        int earned = resolveEarnedPointsForBooking(booking);
        if (earned <= 0) return;
        LoyaltyPoint lp = loyaltyPointRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException("Loyalty account not found for user: " + user.getId()));
        lp.setPoints(lp.getPoints() - earned);
        loyaltyPointRepository.save(lp);
        recordTransaction(user, booking, LoyaltyPointType.REVOKE, -earned, lp.getPoints(), "Revoked earned points from refunded booking");
    }

    @Override
    @Transactional
    public int redeemPointsForBooking(User user, Booking booking, int points) {
        if (points <= 0) {
            return 0;
        }
        LoyaltyPoint lp = getOrCreate(user);
        if (lp.getPoints() < points) {
            throw new BadRequestException("Insufficient points. Available: " + lp.getPoints() + ", requested: " + points);
        }
        redeemPoints(lp, points);
        loyaltyPointRepository.save(lp);
        recordTransaction(user, booking, LoyaltyPointType.REDEEM, -points, lp.getPoints(), "Redeemed for booking " + booking.getBookingCode());
        return calculateRedemptionValue(points).intValue();
    }

    @Override
    @Transactional(readOnly = true)
    public int getMaxRedeemablePointsForAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return 0;
        }
        LoyaltyConfiguration config = getOrCreateConfiguration();
        return amount
                .multiply(BigDecimal.valueOf(config.getRedemptionPoints()))
                .divide(config.getRedemptionValueVnd(), 0, RoundingMode.DOWN)
                .intValue();
    }

    @Override
    @Transactional
    public void restoreRedeemedPointsFromBooking(User user, Booking booking) {
        int redeemed = booking.getLoyaltyPointsRedeemed();
        if (redeemed <= 0) return;
        LoyaltyPoint lp = getOrCreate(user);
        restorePoints(lp, redeemed);
        loyaltyPointRepository.save(lp);
        recordTransaction(user, booking, LoyaltyPointType.RESTORE, redeemed, lp.getPoints(), "Restored redeemed points from refunded booking");
    }

    @Override
    @Transactional
    public LoyaltyConfigurationResponse getConfiguration() {
        return LoyaltyConfigurationResponse.from(getOrCreateConfiguration());
    }

    @Override
    @Transactional
    public LoyaltyConfigurationResponse updateConfiguration(LoyaltyConfigurationRequest request) {
        if (request.earningRatePercent() == null
                || request.earningRatePercent().compareTo(BigDecimal.ZERO) < 0
                || request.earningRatePercent().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Tỷ lệ tích điểm phải nằm trong khoảng 0% đến 100%");
        }
        if (request.redemptionPoints() <= 0) {
            throw new BadRequestException("Số điểm quy đổi phải lớn hơn 0");
        }
        String expiryTime = request.expiryTime() == null || request.expiryTime().isBlank()
                ? "23:59:59"
                : request.expiryTime().trim();
        LocalTime parsedExpiryTime = validateExpiryTime(expiryTime);
        LocalDate parsedExpiryDate = resolveExpiryDate(request);
        validateExpiryDateTime(parsedExpiryDate, parsedExpiryTime);
        LoyaltyConfiguration config = getOrCreateConfiguration();
        config.setEarningRatePercent(request.earningRatePercent());
        config.setRedemptionPoints(request.redemptionPoints());
        config.setRedemptionValueVnd(request.redemptionValueVnd());
        config.setExpiryMonth(parsedExpiryDate.getMonthValue());
        config.setExpiryDay(parsedExpiryDate.getDayOfMonth());
        config.setExpiryTime(expiryTime);
        LoyaltyConfiguration saved = loyaltyConfigurationRepository.save(config);
        deleteDuplicateConfigurations(saved.getId());
        auditLogService.record(AuditActionType.UPDATE, "LOYALTY", saved.getId(), "Cập nhật cấu hình");
        return LoyaltyConfigurationResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoyaltyTransactionResponse> searchTransactions(String keyword, LocalDateTime from, LocalDateTime to, int page, int size) {
        var pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, Math.min(size, 100)),
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))
        );
        return PageResponse.from(loyaltyPointTransactionRepository.findAll(buildTransactionSearchSpec(keyword, from, to), pageable)
                .map(LoyaltyTransactionResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public LoyaltyReportResponse getReport(LocalDateTime from, LocalDateTime to) {
        LocalDateTime safeTo = to == null ? LocalDateTime.now() : to;
        LocalDateTime safeFrom = from == null ? safeTo.minusMonths(1) : from;
        long issued = loyaltyPointTransactionRepository.sumDeltaByTypeBetween(LoyaltyPointType.EARN, safeFrom, safeTo);
        long burned = Math.abs(loyaltyPointTransactionRepository.sumDeltaByTypeBetween(LoyaltyPointType.REDEEM, safeFrom, safeTo));
        long newMembers = userRepository.countByCreatedAtBetween(safeFrom, safeTo);
        double ratio = burned == 0 ? issued : (double) issued / burned;
        return new LoyaltyReportResponse(safeFrom, safeTo, newMembers, issued, burned, ratio);
    }

    @Override
    @Transactional
    public int expireAllActivePoints(String resetSource) {
        int count = 0;
        for (LoyaltyPoint lp : loyaltyPointRepository.findAll()) {
            int currentPoints = lp.getPoints();
            if (currentPoints <= 0) continue;
            lp.setPoints(0);
            loyaltyPointRepository.save(lp);
            recordTransaction(lp.getUser(), null, LoyaltyPointType.EXPIRE, -currentPoints, 0, "Annual loyalty point expiry");
            count++;
        }
        LoyaltyConfiguration config = getOrCreateConfiguration();
        config.setLastExpiredAt(LocalDate.now());
        config.setLastResetAt(LocalDateTime.now());
        config.setLastResetSource(resetSource == null || resetSource.isBlank() ? "SYSTEM" : resetSource.trim().toUpperCase(Locale.ROOT));
        loyaltyConfigurationRepository.save(config);
        auditLogService.record(AuditActionType.UPDATE, "LOYALTY", config.getId(),
                "Reset điểm (" + config.getLastResetSource() + "): " + count + " tài khoản");
        return count;
    }

    private LoyaltyPoint getOrCreate(User user) {
        return loyaltyPointRepository.findByUser(user)
                .orElseGet(() -> loyaltyPointRepository.save(new LoyaltyPoint(user)));
    }

    private LoyaltyConfiguration getOrCreateConfiguration() {
        return loyaltyConfigurationRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> loyaltyConfigurationRepository.save(new LoyaltyConfiguration(true)));
    }

    private void deleteDuplicateConfigurations(Long activeConfigId) {
        loyaltyConfigurationRepository.findAll().stream()
                .filter(config -> !config.getId().equals(activeConfigId))
                .forEach(loyaltyConfigurationRepository::delete);
    }

    private User resolveUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));
    }

    private User resolveUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private void addPoints(LoyaltyPoint loyaltyPoint, int points) {
        loyaltyPoint.setPoints(loyaltyPoint.getPoints() + points);
        loyaltyPoint.setTotalPoints(loyaltyPoint.getTotalPoints() + points);
    }

    private void redeemPoints(LoyaltyPoint loyaltyPoint, int points) {
        loyaltyPoint.setPoints(loyaltyPoint.getPoints() - points);
    }

    private void restorePoints(LoyaltyPoint loyaltyPoint, int points) {
        loyaltyPoint.setPoints(loyaltyPoint.getPoints() + points);
    }

    private int calculateEarnedPoints(Booking booking) {
        if (booking.getTotalAmount() == null || booking.getTotalAmount().signum() <= 0) {
            return 0;
        }
        LoyaltyConfiguration config = getOrCreateConfiguration();
        int earned = booking.getTotalAmount()
                .multiply(config.getEarningRatePercent())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .intValue();
        log.info(
                "Calculated loyalty earning for booking {}: amount={}, ratePercent={}, earnedPoints={}",
                booking.getBookingCode(),
                booking.getTotalAmount(),
                config.getEarningRatePercent(),
                earned
        );
        return earned;
    }

    private int calculateEarnedPoints(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return 0;
        }
        LoyaltyConfiguration config = getOrCreateConfiguration();
        return amount
                .multiply(config.getEarningRatePercent())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .intValue();
    }

    private int resolveEarnedPointsForBooking(Booking booking) {
        if (booking.getId() != null) {
            long earnedFromAuditTrail = loyaltyPointTransactionRepository.sumDeltaByBookingAndType(
                    booking.getId(),
                    LoyaltyPointType.EARN
            );
            if (earnedFromAuditTrail > 0) {
                return Math.toIntExact(earnedFromAuditTrail);
            }
        }
        return calculateEarnedPoints(booking);
    }

    private BigDecimal calculateRedemptionValue(int points) {
        LoyaltyConfiguration config = getOrCreateConfiguration();
        return BigDecimal.valueOf(points)
                .multiply(config.getRedemptionValueVnd())
                .divide(BigDecimal.valueOf(config.getRedemptionPoints()), 0, RoundingMode.DOWN);
    }

    private Specification<LoyaltyPointTransaction> buildTransactionSearchSpec(
            String keyword,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String trimmedKeyword = keyword == null ? "" : keyword.trim();
            if (!trimmedKeyword.isBlank()) {
                String loweredKeyword = trimmedKeyword.toLowerCase(Locale.ROOT);
                String textPattern = "%" + loweredKeyword + "%";
                var userJoin = root.join("user");
                var profileJoin = userJoin.join("profile", JoinType.LEFT);
                var bookingJoin = root.join("booking", JoinType.LEFT);

                List<Predicate> keywordPredicates = new ArrayList<>();
                keywordPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("email")), textPattern));
                keywordPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(profileJoin.get("fullName")), textPattern));
                keywordPredicates.add(criteriaBuilder.like(profileJoin.get("phone"), "%" + trimmedKeyword + "%"));
                keywordPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(bookingJoin.get("bookingCode")), textPattern));
                try {
                    keywordPredicates.add(criteriaBuilder.equal(userJoin.get("id"), Long.valueOf(trimmedKeyword)));
                } catch (NumberFormatException ignored) {
                    // Keyword is not a customer ID; other text filters still apply.
                }
                predicates.add(criteriaBuilder.or(keywordPredicates.toArray(Predicate[]::new)));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private LocalDate resolveExpiryDate(LoyaltyConfigurationRequest request) {
        if (request.expiryDate() != null && !request.expiryDate().isBlank()) {
            try {
                return LocalDate.parse(request.expiryDate().trim());
            } catch (RuntimeException ex) {
                throw new BadRequestException("Ngày reset điểm phải đúng định dạng yyyy-MM-dd");
            }
        }
        validateExpiryDate(request.expiryMonth(), request.expiryDay());
        return LocalDate.of(LocalDate.now().getYear(), request.expiryMonth(), request.expiryDay());
    }

    private void validateExpiryDate(int month, int day) {
        int currentYear = LocalDate.now().getYear();
        int maxDay = YearMonth.of(currentYear, month).lengthOfMonth();
        if (day > maxDay) {
            throw new BadRequestException("Ngày reset điểm không hợp lệ với tháng đã chọn");
        }
    }

    private LocalTime validateExpiryTime(String expiryTime) {
        try {
            return LocalTime.parse(expiryTime);
        } catch (RuntimeException ex) {
            throw new BadRequestException("Giờ reset điểm phải đúng định dạng HH:mm:ss");
        }
    }

    private void validateExpiryDateTime(LocalDate expiryDate, LocalTime expiryTime) {
        LocalDateTime minimumAllowed = LocalDateTime.now().plusMinutes(MIN_EXPIRY_LEAD_MINUTES);
        if (LocalDateTime.of(expiryDate, expiryTime).isBefore(minimumAllowed)) {
            throw new BadRequestException("Ngày và giờ reset điểm phải sau thời điểm hiện tại ít nhất 15 phút");
        }
    }

    private void recordTransaction(User user, Booking booking, LoyaltyPointType type, int pointsDelta, int balanceAfter, String note) {
        if (pointsDelta == 0) return;
        loyaltyPointTransactionRepository.save(new LoyaltyPointTransaction(
                user,
                booking,
                type,
                pointsDelta,
                balanceAfter,
                note == null ? null : note.substring(0, Math.min(500, note.length()))
        ));
    }
}
