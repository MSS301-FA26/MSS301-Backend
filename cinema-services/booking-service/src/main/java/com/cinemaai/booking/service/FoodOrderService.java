package com.cinemaai.booking.service;

import com.cinemaai.booking.dto.request.FoodOrderRequest;
import com.cinemaai.booking.dto.response.FoodOrderResponse;
import java.util.List;

public interface FoodOrderService {
    FoodOrderResponse createStandalone(Long userId, FoodOrderRequest request);
    FoodOrderResponse createForBooking(Long userId, Long bookingId, FoodOrderRequest request);
    List<FoodOrderResponse> listMine(Long userId);
    List<FoodOrderResponse> listByBooking(Long userId, Long bookingId);
    FoodOrderResponse cancel(Long userId, Long foodOrderId);
    FoodOrderResponse getById(Long foodOrderId);
    FoodOrderResponse getByIdAndUser(Long userId, Long foodOrderId);
    FoodOrderResponse markPaid(Long foodOrderId, String transactionId);
    FoodOrderResponse syncPaymentStatus(Long userId, Long foodOrderId);
}