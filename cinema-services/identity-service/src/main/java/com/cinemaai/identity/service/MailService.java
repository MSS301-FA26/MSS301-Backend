package com.cinemaai.identity.service;

public interface MailService {

    void sendOtp(String to, String otp, String purpose);
}
