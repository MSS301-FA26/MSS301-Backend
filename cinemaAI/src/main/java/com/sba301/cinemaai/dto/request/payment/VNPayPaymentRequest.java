package com.sba301.cinemaai.dto.request.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VNPayPaymentRequest {

    private Long bookingId;
    private Long amount;
    private String orderInfo;
    private String orderType;
    private String locale;
    private String bankCode;
}
