package com.cinemaai.payment.service;

import com.cinemaai.payment.dto.request.CreateRefundBatchRequest;
import com.cinemaai.payment.dto.request.CreateRefundRequest;
import com.cinemaai.payment.dto.response.PageResponse;
import com.cinemaai.payment.dto.response.RefundBatchResponse;
import com.cinemaai.payment.dto.response.RefundResponse;
import java.math.BigDecimal;

public interface RefundService {

    RefundResponse processSingleRefund(CreateRefundRequest request, Long actorUserId);

    RefundResponse processRefundForBooking(Long bookingId, BigDecimal amount, String reason, Long actorUserId);

    RefundBatchResponse processRefundBatch(CreateRefundBatchRequest request, Long actorUserId);

    RefundResponse getRefundById(Long refundId);

    PageResponse<RefundResponse> getMyRefunds(Long userId, int page, int size);

    PageResponse<RefundResponse> getAllRefunds(String status, int page, int size);

    PageResponse<RefundBatchResponse> getRefundBatches(Long cinemaId, String status, int page, int size);
}
