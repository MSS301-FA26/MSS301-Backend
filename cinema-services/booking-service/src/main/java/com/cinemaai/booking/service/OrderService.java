package com.cinemaai.booking.service;

import com.cinemaai.booking.dto.request.OrderRequest;
import com.cinemaai.booking.dto.response.OrderResponse;
import com.cinemaai.booking.dto.response.PageResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse createStandalone(Long userId, OrderRequest request);
    OrderResponse createForBooking(Long userId, Long bookingId, OrderRequest request);
    List<OrderResponse> listMine(Long userId);
    PageResponse<OrderResponse> listAll(Pageable pageable);
    List<OrderResponse> listByBooking(Long userId, Long bookingId);
    OrderResponse cancel(Long userId, Long foodOrderId);
    OrderResponse getById(Long foodOrderId);
    OrderResponse getByIdAndUser(Long userId, Long foodOrderId);
    OrderResponse markPaid(Long foodOrderId, String transactionId);
    OrderResponse syncPaymentStatus(Long userId, Long foodOrderId);
}
