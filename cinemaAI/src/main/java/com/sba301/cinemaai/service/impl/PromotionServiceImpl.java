package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.promotion.PromotionRequest;
import com.sba301.cinemaai.dto.request.promotion.ValidateVoucherRequest;
import com.sba301.cinemaai.dto.response.promotion.PromotionResponse;
import com.sba301.cinemaai.dto.response.promotion.PromotionStatsResponse;
import com.sba301.cinemaai.dto.response.promotion.ValidateVoucherResponse;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.FoodOrder;
import com.sba301.cinemaai.entity.Promotion;
import com.sba301.cinemaai.entity.PromotionUsage;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.DiscountType;
import com.sba301.cinemaai.enums.PromotionStatus;
import com.sba301.cinemaai.enums.PromotionTarget;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.BookingRepository;
import com.sba301.cinemaai.repository.FoodOrderRepository;
import com.sba301.cinemaai.repository.PromotionRepository;
import com.sba301.cinemaai.repository.PromotionUsageRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final FoodOrderRepository foodOrderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> getAllPromotions(boolean includeDeleted) {
        List<Promotion> promotions = includeDeleted
                ? promotionRepository.findAll()
                : promotionRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc();
        return promotions.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> getActivePublicPromotions() {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findActivePromotions(now).stream()
                .filter(p -> p.getUsageLimit() == null || p.getUsedCount() < p.getUsageLimit())
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotionById(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mã khuyến mãi với ID: " + id));
        return mapToResponse(promotion);
    }

    @Override
    @Transactional
    public PromotionResponse createPromotion(PromotionRequest request) {
        String normalizedCode = request.code().trim().toUpperCase();

        if (promotionRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(normalizedCode)) {
            throw new BadRequestException("Mã khuyến mãi đã tồn tại: " + normalizedCode);
        }

        if (request.startDate().isAfter(request.endDate())) {
            throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        Promotion promotion = Promotion.builder()
                .code(normalizedCode)
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .minOrderValue(request.minOrderValue() != null ? request.minOrderValue() : BigDecimal.ZERO)
                .maxDiscountAmount(request.maxDiscountAmount())
                .usageLimit(request.usageLimit())
                .usedCount(0)
                .userUsageLimit(request.userUsageLimit() != null && request.userUsageLimit() > 0 ? request.userUsageLimit() : 1)
                .applicableTarget(request.applicableTarget() != null ? request.applicableTarget() : PromotionTarget.ALL)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(request.status() != null ? request.status() : PromotionStatus.ACTIVE)
                .build();

        Promotion saved = promotionRepository.save(promotion);
        log.info("Created promotion code: {}", saved.getCode());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public PromotionResponse updatePromotion(Long id, PromotionRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mã khuyến mãi với ID: " + id));

        String normalizedCode = request.code().trim().toUpperCase();
        if (promotionRepository.existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(normalizedCode, id)) {
            throw new BadRequestException("Mã khuyến mãi đã được sử dụng bởi chương trình khác: " + normalizedCode);
        }

        if (request.startDate().isAfter(request.endDate())) {
            throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        promotion.setCode(normalizedCode);
        promotion.setName(request.name().trim());
        promotion.setDescription(request.description() != null ? request.description().trim() : null);
        promotion.setDiscountType(request.discountType());
        promotion.setDiscountValue(request.discountValue());
        promotion.setMinOrderValue(request.minOrderValue() != null ? request.minOrderValue() : BigDecimal.ZERO);
        promotion.setMaxDiscountAmount(request.maxDiscountAmount());
        promotion.setUsageLimit(request.usageLimit());
        promotion.setUserUsageLimit(request.userUsageLimit() != null && request.userUsageLimit() > 0 ? request.userUsageLimit() : 1);
        promotion.setApplicableTarget(request.applicableTarget() != null ? request.applicableTarget() : PromotionTarget.ALL);
        promotion.setStartDate(request.startDate());
        promotion.setEndDate(request.endDate());
        if (request.status() != null) {
            promotion.setStatus(request.status());
        }

        Promotion updated = promotionRepository.save(promotion);
        log.info("Updated promotion ID {}: {}", id, updated.getCode());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public PromotionResponse togglePromotionStatus(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mã khuyến mãi với ID: " + id));

        promotion.setStatus(promotion.getStatus() == PromotionStatus.ACTIVE ? PromotionStatus.INACTIVE : PromotionStatus.ACTIVE);
        Promotion updated = promotionRepository.save(promotion);
        log.info("Toggled promotion ID {} status to: {}", id, updated.getStatus());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mã khuyến mãi với ID: " + id));

        promotion.setDeletedAt(LocalDateTime.now());
        promotionRepository.save(promotion);
        log.info("Soft deleted promotion ID: {}", id);
    }

    @Override
    @Transactional
    public PromotionResponse restorePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mã khuyến mãi với ID: " + id));

        promotion.setDeletedAt(null);
        Promotion restored = promotionRepository.save(promotion);
        log.info("Restored promotion ID: {}", id);
        return mapToResponse(restored);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidateVoucherResponse validateVoucher(ValidateVoucherRequest request, Long userId) {
        String normalizedCode = request.code().trim().toUpperCase();

        var opt = promotionRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(normalizedCode);
        if (opt.isEmpty()) {
            return ValidateVoucherResponse.invalid(normalizedCode, "Mã khuyến mãi không tồn tại.");
        }

        Promotion p = opt.get();
        LocalDateTime now = LocalDateTime.now();

        if (p.getStatus() != PromotionStatus.ACTIVE) {
            return ValidateVoucherResponse.invalid(normalizedCode, "Mã khuyến mãi đang tạm ngưng áp dụng.");
        }

        if (now.isBefore(p.getStartDate())) {
            return ValidateVoucherResponse.invalid(normalizedCode, "Chương trình ưu đãi chưa bắt đầu.");
        }

        if (now.isAfter(p.getEndDate())) {
            return ValidateVoucherResponse.invalid(normalizedCode, "Mã khuyến mãi đã hết hạn sử dụng.");
        }

        if (p.getUsageLimit() != null && p.getUsedCount() >= p.getUsageLimit()) {
            return ValidateVoucherResponse.invalid(normalizedCode, "Mã khuyến mãi đã hết lượt sử dụng.");
        }

        if (userId != null) {
            long usedByUser = promotionUsageRepository.countByPromotionIdAndUserId(p.getId(), userId);
            if (usedByUser >= p.getUserUsageLimit()) {
                return ValidateVoucherResponse.invalid(normalizedCode,
                        "Bạn đã dùng hết số lượt cho phép của mã này (Tối đa " + p.getUserUsageLimit() + " lần).");
            }
        }

        if (request.orderAmount().compareTo(p.getMinOrderValue()) < 0) {
            return ValidateVoucherResponse.invalid(normalizedCode,
                    "Đơn hàng cần đạt tối thiểu " + formatVnd(p.getMinOrderValue()) + " để áp dụng mã.");
        }

        if (request.target() != null && p.getApplicableTarget() != PromotionTarget.ALL) {
            if (p.getApplicableTarget() != request.target()) {
                if (p.getApplicableTarget() == PromotionTarget.TICKET_ONLY) {
                    return ValidateVoucherResponse.invalid(normalizedCode, "Mã này chỉ áp dụng cho vé xem phim.");
                } else if (p.getApplicableTarget() == PromotionTarget.FOOD_ONLY) {
                    return ValidateVoucherResponse.invalid(normalizedCode, "Mã này chỉ áp dụng cho bắp nước F&B.");
                }
            }
        }

        // Calculate discount
        BigDecimal discount;
        if (p.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = p.getDiscountValue().min(request.orderAmount());
        } else {
            discount = request.orderAmount().multiply(p.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (p.getMaxDiscountAmount() != null && p.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(p.getMaxDiscountAmount());
            }
            discount = discount.min(request.orderAmount());
        }

        BigDecimal finalAmount = request.orderAmount().subtract(discount).max(BigDecimal.ZERO);
        String msg = "Áp dụng thành công! Bạn được giảm " + formatVnd(discount);

        return ValidateVoucherResponse.valid(
                normalizedCode,
                p.getName(),
                p.getDiscountType(),
                p.getDiscountValue(),
                discount,
                finalAmount,
                msg
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionStatsResponse getPromotionStats() {
        LocalDateTime now = LocalDateTime.now();
        long total = promotionRepository.countTotalPromotions();
        long active = promotionRepository.countActivePromotions(now);
        long usages = promotionUsageRepository.countTotalUsages();
        BigDecimal discountGiven = promotionUsageRepository.sumTotalDiscountGiven();

        return new PromotionStatsResponse(total, active, usages, discountGiven != null ? discountGiven : BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public void recordPromotionUsage(String code, Long userId, Long bookingId, Long foodOrderId, BigDecimal discountAmount) {
        if (code == null || code.isBlank() || discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String normalizedCode = code.trim().toUpperCase();
        var opt = promotionRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(normalizedCode);
        if (opt.isEmpty()) return;

        Promotion p = opt.get();
        p.setUsedCount(p.getUsedCount() + 1);
        promotionRepository.save(p);

        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        Booking booking = bookingId != null ? bookingRepository.findById(bookingId).orElse(null) : null;
        FoodOrder foodOrder = foodOrderId != null ? foodOrderRepository.findById(foodOrderId).orElse(null) : null;

        if (user != null) {
            PromotionUsage usage = PromotionUsage.builder()
                    .promotion(p)
                    .user(user)
                    .booking(booking)
                    .foodOrder(foodOrder)
                    .discountAmount(discountAmount)
                    .usedAt(LocalDateTime.now())
                    .build();
            promotionUsageRepository.save(usage);
            log.info("Recorded promotion usage: code={}, user={}, discount={}", normalizedCode, userId, discountAmount);
        }
    }

    private PromotionResponse mapToResponse(Promotion p) {
        LocalDateTime now = LocalDateTime.now();
        boolean isValid = p.getDeletedAt() == null
                && p.getStatus() == PromotionStatus.ACTIVE
                && !now.isBefore(p.getStartDate())
                && !now.isAfter(p.getEndDate())
                && (p.getUsageLimit() == null || p.getUsedCount() < p.getUsageLimit());

        return new PromotionResponse(
                p.getId(),
                p.getCode(),
                p.getName(),
                p.getDescription(),
                p.getDiscountType(),
                p.getDiscountValue(),
                p.getMinOrderValue(),
                p.getMaxDiscountAmount(),
                p.getUsageLimit(),
                p.getUsedCount(),
                p.getUserUsageLimit(),
                p.getApplicableTarget(),
                p.getStartDate(),
                p.getEndDate(),
                p.getStatus(),
                isValid,
                p.getDeletedAt(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    private String formatVnd(BigDecimal amount) {
        return NumberFormatHolder.formatVnd(amount);
    }

    private static class NumberFormatHolder {
        static String formatVnd(BigDecimal amount) {
            if (amount == null) return "0đ";
            return String.format("%,dđ", amount.longValue());
        }
    }
}
