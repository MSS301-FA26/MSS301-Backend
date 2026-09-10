package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.auth.GoogleLoginRequest;
import com.sba301.cinemaai.dto.request.auth.GoogleOtpVerifyRequest;
import com.sba301.cinemaai.dto.request.auth.LoginRequest;
import com.sba301.cinemaai.dto.request.auth.RegisterRequest;
import com.sba301.cinemaai.dto.response.auth.AuthResponse;
import com.sba301.cinemaai.dto.response.auth.RegisterResponse;

public interface AuthService {

        public RegisterResponse register(RegisterRequest request);

        public void resendVerificationOtp(String email);

        public void verifyEmail(String email, String otp);

        public AuthResponse login(LoginRequest request);

        public AuthResponse loginWithGoogle(GoogleLoginRequest request);

        public AuthResponse loginWithGoogleOtp(GoogleOtpVerifyRequest request);

        public AuthResponse refresh(String refreshTokenValue);

        public void logout(String refreshTokenValue);
}
