package com.cinemaai.payment.controller;

import com.cinemaai.payment.dto.request.AwardBookingPointsRequest;
import com.cinemaai.payment.dto.request.RefundBookingPointsRequest;
import com.cinemaai.payment.dto.response.ApiResponse;
import com.cinemaai.payment.dto.response.LoyaltyConfigurationResponse;
import com.cinemaai.payment.dto.response.LoyaltyResponse;
import com.cinemaai.payment.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/loyalty")
@RequiredArgsConstructor
@Tag(name = "Internal Loyalty API", description = "API nội bộ tích điểm và hoàn điểm thưởng khi đặt vé")
public class InternalLoyaltyController {

    private final LoyaltyService loyaltyService;

    @Operation(summary = "Lấy cấu hình loyalty theo rạp phục vụ tính giảm giá vé nội bộ (Internal)")
    @GetMapping("/config")
    public ApiResponse<LoyaltyConfigurationResponse> getConfigurationInternal(@RequestParam(required = false) Long cinemaId) {
        return ApiResponse.success(loyaltyService.getConfiguration(cinemaId));
    }

    @Operation(summary = "Tích điểm thưởng sau khi đơn đặt vé thanh toán thành công (Internal)")
    @PostMapping("/award")
    public ApiResponse<LoyaltyResponse> awardPoints(@RequestBody AwardBookingPointsRequest request) {
        return ApiResponse.success(
                loyaltyService.awardPointsForBooking(request),
                "Tích điểm thưởng cho đơn đặt vé thành công"
        );
    }

    @Operation(summary = "Hoàn lại điểm đã đổi và thu hồi điểm đã tích khi vé bị hủy/hoàn tiền (Internal)")
    @PostMapping("/refund")
    public ApiResponse<LoyaltyResponse> refundPoints(@RequestBody RefundBookingPointsRequest request) {
        return ApiResponse.success(
                loyaltyService.refundPointsForBooking(request),
                "Hoàn và thu hồi điểm thưởng thành công"
        );
    }
}
