package com.cinemaai.payment.controller;

import com.cinemaai.payment.dto.request.CreateRefundBatchRequest;
import com.cinemaai.payment.dto.response.ApiResponse;
import com.cinemaai.payment.dto.response.RefundBatchResponse;
import com.cinemaai.payment.dto.response.RefundResponse;
import com.cinemaai.payment.service.RefundService;
import io.swagger.v3.oas.annotations.Hidden;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Hidden
@RestController
@RequestMapping("/internal/v1/refunds")
@RequiredArgsConstructor
public class InternalRefundController {

    private final RefundService refundService;

    @PostMapping("/batch")
    public ApiResponse<RefundBatchResponse> processBatch(
            @RequestBody CreateRefundBatchRequest request,
            @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId
    ) {
        return ApiResponse.success(
                refundService.processRefundBatch(request, actorUserId != null ? actorUserId : 1L),
                "Xử lý hoàn tiền theo lô thành công"
        );
    }

    @PostMapping("/booking/{bookingId}")
    public ApiResponse<RefundResponse> processBookingRefund(
            @PathVariable Long bookingId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId
    ) {
        return ApiResponse.success(
                refundService.processRefundForBooking(bookingId, amount, reason, actorUserId != null ? actorUserId : 1L),
                "Hoàn tiền đơn đặt vé thành công"
        );
    }
}
