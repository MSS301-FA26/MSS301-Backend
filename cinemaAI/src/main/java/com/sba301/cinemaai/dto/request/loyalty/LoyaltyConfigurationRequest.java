package com.sba301.cinemaai.dto.request.loyalty;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LoyaltyConfigurationRequest(
        @NotNull(message = "Tỷ lệ tích điểm là bắt buộc")
        @DecimalMin(value = "0.00", message = "Tỷ lệ tích điểm không được âm")
        @DecimalMax(value = "100.00", message = "Tỷ lệ tích điểm không được vượt quá 100%")
        BigDecimal earningRatePercent,

        @Min(value = 1, message = "Số điểm quy đổi phải lớn hơn hoặc bằng 1")
        int redemptionPoints,

        @NotNull(message = "Giá trị giảm là bắt buộc")
        @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
        BigDecimal redemptionValueVnd,

        @Min(value = 1, message = "Tháng reset điểm phải nằm trong khoảng 1 đến 12")
        @Max(value = 12, message = "Tháng reset điểm phải nằm trong khoảng 1 đến 12")
        int expiryMonth,

        @Min(value = 1, message = "Ngày reset điểm phải nằm trong khoảng 1 đến 31")
        @Max(value = 31, message = "Ngày reset điểm phải nằm trong khoảng 1 đến 31")
        int expiryDay,

        String expiryDate,

        String expiryTime
) {
}
