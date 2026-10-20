package com.cinemaai.identity.service;

import com.cinemaai.identity.entity.PasswordResetToken;

public interface PasswordResetService {

    PasswordResetToken request(String email);

    void confirm(String email, String otp, String newPassword, String confirmPassword);

    void verifyOtp(String email, String otp);
}
