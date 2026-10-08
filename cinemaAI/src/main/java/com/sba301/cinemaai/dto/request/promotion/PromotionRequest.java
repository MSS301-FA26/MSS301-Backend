package com.sba301.cinemaai.dto.request.promotion;

import com.sba301.cinemaai.enums.DiscountType;
import com.sba301.cinemaai.enums.PromotionStatus;
import com.sba301.cinemaai.enums.PromotionTarget;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionRequest(
        @NotBlank(message = "Mã khuyến mãi không được để trống")
        @Size(min = 2, max = 50, message = "Mã khuyến mãi từ 2 đến 50 ký tự")
        String code,

        @NotBlank(message = "Tên chương trình ưu đãi không được để trống")
        @Size(max = 255, message = "Tên chương trình không quá 255 ký tự")
        String name,

        String description,

        @NotNull(message = "Loại giảm giá là bắt buộc")
        DiscountType discountType,

        @NotNull(message = "Giá trị giảm giá là bắt buộc")
        @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
        BigDecimal discountValue,

        BigDecimal minOrderValue,

        BigDecimal maxDiscountAmount,

        Integer usageLimit,

        Integer userUsageLimit,

        @NotNull(message = "Phạm vi áp dụng là bắt buộc")
        PromotionTarget applicableTarget,

        @NotNull(message = "Ngày bắt đầu là bắt buộc")
        LocalDateTime startDate,

        @NotNull(message = "Ngày kết thúc là bắt buộc")
        LocalDateTime endDate,

        PromotionStatus status
) {
}
