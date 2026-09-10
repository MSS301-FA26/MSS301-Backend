package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.response.auth.AuthResponse;
import com.sba301.cinemaai.dto.request.auth.EmailOtpRequest;
import com.sba301.cinemaai.dto.request.auth.EmailVerificationRequest;
import com.sba301.cinemaai.dto.request.auth.GoogleLoginRequest;
import com.sba301.cinemaai.dto.request.auth.GoogleOtpVerifyRequest;
import com.sba301.cinemaai.dto.request.auth.LoginRequest;
import com.sba301.cinemaai.dto.request.auth.LogoutRequest;
import com.sba301.cinemaai.dto.request.auth.PasswordResetConfirmRequest;
import com.sba301.cinemaai.dto.request.auth.PasswordResetOtpVerifyRequest;
import com.sba301.cinemaai.dto.request.auth.PasswordResetRequest;
import com.sba301.cinemaai.dto.request.auth.RefreshTokenRequest;
import com.sba301.cinemaai.dto.request.auth.RegisterRequest;
import com.sba301.cinemaai.dto.response.auth.RegisterResponse;
import com.sba301.cinemaai.dto.response.auth.TokenResponse;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.service.AuthService;
import com.sba301.cinemaai.service.PasswordResetService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request), "Registered successfully");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), "Logged in successfully");
    }

    @PostMapping("/google")
    public ApiResponse<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ApiResponse.success(authService.loginWithGoogle(request), "Logged in with Google successfully");
    }

    @PostMapping("/google/verify")
    public ApiResponse<AuthResponse> loginWithGoogleOtp(@Valid @RequestBody GoogleOtpVerifyRequest request) {
        return ApiResponse.success(authService.loginWithGoogleOtp(request), "Logged in with Google successfully");
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()), "Token refreshed successfully");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.success(null, "Logged out successfully");
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        authService.verifyEmail(request.email(), request.otp());
        return ApiResponse.success(null, "Email verified successfully");
    }

    @PostMapping("/verify-email/request")
    public ApiResponse<Void> requestEmailVerificationOtp(@Valid @RequestBody EmailOtpRequest request) {
        authService.resendVerificationOtp(request.email());
        return ApiResponse.success(null, "Email verification OTP sent");
    }

    @PostMapping("/password-reset/request")
    public ApiResponse<TokenResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        return ApiResponse.success(
                new TokenResponse(passwordResetService.request(request.email()).getToken()),
                "Password reset OTP sent"
        );
    }

    @PostMapping("/password-reset/verify")
    public ApiResponse<Void> verifyPasswordResetOtp(@Valid @RequestBody PasswordResetOtpVerifyRequest request) {
        passwordResetService.verifyOtp(request.email(), request.otp());
        return ApiResponse.success(null, "Password reset OTP verified");
    }

    @PostMapping("/password-reset/confirm")
    public ApiResponse<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirm(
                request.email(),
                request.otp(),
                request.newPassword(),
                request.confirmPassword()
        );
        return ApiResponse.success(null, "Password reset successfully");
    }
}
