package com.cinemaai.identity.service;

import java.math.BigDecimal;

public interface MailService {

    void sendOtp(String to, String otp, String purpose);

    void sendWalletRefundNotice(String to, String bookingCode, BigDecimal amount, BigDecimal newBalance, String reason);
}
