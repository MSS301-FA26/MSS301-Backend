package com.sba301.cinemaai.dto.request.promotion;

import com.sba301.cinemaai.enums.PromotionTarget;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ValidateVoucherRequest(
        @NotBlank(message = "Mã voucher không được để trống")
        String code,

        @NotNull(message = "Giá trị đơn hàng không được để trống")
        @DecimalMin(value = "0.0", message = "Giá trị đơn hàng phải >= 0")
        BigDecimal orderAmount,

        PromotionTarget target
) {
}
