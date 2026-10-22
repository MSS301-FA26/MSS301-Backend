package com.cinemaai.payment.mapper;

import com.cinemaai.payment.dto.response.PaymentResponse;
import com.cinemaai.payment.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toResponse(Payment payment);
}
