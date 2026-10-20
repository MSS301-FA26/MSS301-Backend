package com.cinemaai.identity.service;

import com.cinemaai.identity.dto.request.auth.GoogleLoginRequest;
import com.cinemaai.identity.dto.request.auth.GoogleOtpVerifyRequest;
import com.cinemaai.identity.dto.request.auth.LoginRequest;
import com.cinemaai.identity.dto.request.auth.RegisterRequest;
import com.cinemaai.identity.dto.response.auth.AuthResponse;
import com.cinemaai.identity.dto.response.auth.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    void resendVerificationOtp(String email);

    void verifyEmail(String email, String otp);

    AuthResponse login(LoginRequest request);

    AuthResponse loginWithGoogle(GoogleLoginRequest request);

    AuthResponse loginWithGoogleOtp(GoogleOtpVerifyRequest request);

    AuthResponse refresh(String refreshTokenValue);

    void logout(String refreshTokenValue);
}
