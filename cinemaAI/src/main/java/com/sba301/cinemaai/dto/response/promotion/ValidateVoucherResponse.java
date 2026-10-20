package com.sba301.cinemaai.dto.response.promotion;

import com.sba301.cinemaai.enums.DiscountType;

import java.math.BigDecimal;

public record ValidateVoucherResponse(
        boolean valid,
        String code,
        String name,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String message
) {
    public static ValidateVoucherResponse invalid(String code, String message) {
        return new ValidateVoucherResponse(false, code, null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, message);
    }

    public static ValidateVoucherResponse valid(String code, String name, DiscountType discountType, BigDecimal discountValue, BigDecimal discountAmount, BigDecimal finalAmount, String message) {
        return new ValidateVoucherResponse(true, code, name, discountType, discountValue, discountAmount, finalAmount, message);
    }
}
