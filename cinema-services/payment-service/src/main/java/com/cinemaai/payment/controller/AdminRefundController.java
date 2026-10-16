package com.cinemaai.payment.controller;

import com.cinemaai.payment.dto.request.CreateRefundBatchRequest;
import com.cinemaai.payment.dto.request.CreateRefundRequest;
import com.cinemaai.payment.dto.response.ApiResponse;
import com.cinemaai.payment.dto.response.PageResponse;
import com.cinemaai.payment.dto.response.RefundBatchResponse;
import com.cinemaai.payment.dto.response.RefundResponse;
import com.cinemaai.payment.security.AuthenticatedUser;
import com.cinemaai.payment.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/admin/refunds", "/api/v1/admin/refund"})
@RequiredArgsConstructor
@Tag(name = "Admin - Refunds", description = "Quản lý hoàn tiền sự cố và hoàn tiền theo lô của rạp")
public class AdminRefundController {

    private final RefundService refundService;

    @Operation(summary = "Tạo lệnh hoàn tiền đơn lẻ cho khách hàng")
    @PostMapping
    public ApiResponse<RefundResponse> createSingleRefund(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody CreateRefundRequest request
    ) {
        Long actorUserId = user != null ? user.id() : 1L;
        return ApiResponse.success(refundService.processSingleRefund(request, actorUserId), "Tạo hoàn tiền thành công");
    }

    @Operation(summary = "Tạo lệnh hoàn tiền theo lô cho toàn bộ suất chiếu sự cố")
    @PostMapping("/batch")
    public ApiResponse<RefundBatchResponse> createRefundBatch(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody CreateRefundBatchRequest request
    ) {
        Long actorUserId = user != null ? user.id() : 1L;
        return ApiResponse.success(refundService.processRefundBatch(request, actorUserId), "Tạo hoàn tiền theo lô thành công");
    }

    @Operation(summary = "Xem chi tiết một giao dịch hoàn tiền")
    @GetMapping("/{refundId}")
    public ApiResponse<RefundResponse> getRefundById(@PathVariable Long refundId) {
        return ApiResponse.success(refundService.getRefundById(refundId));
    }

    @Operation(summary = "Danh sách tất cả các lệnh hoàn tiền")
    @GetMapping
    public ApiResponse<PageResponse<RefundResponse>> getAllRefunds(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(refundService.getAllRefunds(status, page, size));
    }

    @Operation(summary = "Danh sách các lô hoàn tiền sự cố (Refund Batches)")
    @GetMapping("/batches")
    public ApiResponse<PageResponse<RefundBatchResponse>> getRefundBatches(
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(refundService.getRefundBatches(cinemaId, status, page, size));
    }
}
