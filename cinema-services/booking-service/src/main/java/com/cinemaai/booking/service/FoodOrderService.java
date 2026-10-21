package com.cinemaai.booking.service;

import com.cinemaai.booking.dto.request.FoodOrderRequest;
import com.cinemaai.booking.dto.response.FoodOrderResponse;
import com.cinemaai.booking.dto.response.PageResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface FoodOrderService {
    FoodOrderResponse createStandalone(Long userId, FoodOrderRequest request);
    FoodOrderResponse createForBooking(Long userId, Long bookingId, FoodOrderRequest request);
    List<FoodOrderResponse> listMine(Long userId);
    PageResponse<FoodOrderResponse> listAll(Pageable pageable);
    List<FoodOrderResponse> listByBooking(Long userId, Long bookingId);
    FoodOrderResponse cancel(Long userId, Long foodOrderId);
    FoodOrderResponse getById(Long foodOrderId);
    FoodOrderResponse getByIdAndUser(Long userId, Long foodOrderId);
    FoodOrderResponse markPaid(Long foodOrderId, String transactionId);
    FoodOrderResponse syncPaymentStatus(Long userId, Long foodOrderId);
}
