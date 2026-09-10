package com.sba301.cinemaai.dto.response.payment;

public record VnpayCallbackParams(
        String vnp_TxnRef,
        String vnp_ResponseCode,
        String vnp_TransactionStatus,
        String vnp_TransactionNo,
        String vnp_Amount,
        String vnp_OrderInfo,
        String vnp_BankCode,
        String vnp_PayDate,
        String vnp_SecureHash
) {
}
