package com.cinemaai.identity.service;

import com.cinemaai.identity.entity.EmailVerificationToken;
import com.cinemaai.identity.entity.User;

public interface EmailVerificationService {

    EmailVerificationToken create(User user);

    EmailVerificationToken createGoogleLoginOtp(User user);

    void resendVerificationOtp(String email);

    void verifyEmail(String email, String otp);

    User verifyGoogleLogin(String email, String otp);

    void verify(String token);
}
