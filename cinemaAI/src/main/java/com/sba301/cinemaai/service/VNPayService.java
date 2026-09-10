package com.sba301.cinemaai.service;

import java.math.BigDecimal;
import java.util.Map;
import java.time.LocalDateTime;

public interface VNPayService {

        public String buildPaymentUrl(String txnRef, BigDecimal amount, String orderInfo, String clientIp);

        public String buildPaymentUrl(String txnRef, BigDecimal amount, String orderInfo, String clientIp,
                                      LocalDateTime expiresAt);

        public boolean verifySignature(Map<String, String> params);
}
