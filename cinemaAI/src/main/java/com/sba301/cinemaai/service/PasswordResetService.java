package com.sba301.cinemaai.service;

import com.sba301.cinemaai.entity.PasswordResetToken;

public interface PasswordResetService {

        public PasswordResetToken request(String email);

        public void verifyOtp(String email, String otp);

        public void confirm(String email, String otp, String newPassword, String confirmPassword);
}
