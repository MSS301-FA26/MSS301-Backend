package com.sba301.cinemaai.dto.response.payment;

import com.sba301.cinemaai.util.MessageTranslator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VNPayPaymentResponse {

    private String code;
    private String message;
    private String paymentUrl;

    public String getMessage() {
        return MessageTranslator.translate(message);
    }
}
