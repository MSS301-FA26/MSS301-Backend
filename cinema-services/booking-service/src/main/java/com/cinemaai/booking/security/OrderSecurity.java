package com.cinemaai.booking.security;

import com.cinemaai.booking.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("orderSecurity")
@RequiredArgsConstructor
public class OrderSecurity {
    private final OrderRepository orderRepository;

    public boolean isOwner(Long foodOrderId, Long userId) {
        if (foodOrderId == null || userId == null) return false;
        return orderRepository.findByIdAndUserId(foodOrderId, userId).isPresent();
    }
}
