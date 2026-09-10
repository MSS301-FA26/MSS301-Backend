package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.entity.PasswordResetToken;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.PasswordResetTokenRepository;
import com.sba301.cinemaai.repository.UserRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sba301.cinemaai.service.PasswordResetService;
import com.sba301.cinemaai.service.MailService;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int PASSWORD_RESET_OTP_EXPIRES_IN_MINUTES = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Transactional
    public PasswordResetToken request(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        PasswordResetToken resetToken = passwordResetTokenRepository.save(
                new PasswordResetToken(
                        user,
                        generateOtp(),
                        LocalDateTime.now().plusMinutes(PASSWORD_RESET_OTP_EXPIRES_IN_MINUTES)
                )
        );
        mailService.sendOtp(user.getEmail(), resetToken.getToken(), "Đặt lại mật khẩu");
        return resetToken;
    }

    @Transactional
    public void confirm(String email, String otp, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequestException("Confirm password does not match");
        }

        PasswordResetToken resetToken = findValidResetToken(email, otp);

        resetToken.getUser().setPasswordHash(passwordEncoder.encode(newPassword));
        resetToken.setUsed(true);
    }

    @Transactional(readOnly = true)
    public void verifyOtp(String email, String otp) {
        findValidResetToken(email, otp);
    }

    private PasswordResetToken findValidResetToken(String email, String otp) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findFirstByUserEmailAndTokenAndUsedFalseOrderByCreatedAtDesc(email, otp)
                .orElseThrow(() -> new BadRequestException("Invalid password reset OTP"));
        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Password reset OTP is expired or already used");
        }
        return resetToken;
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }
}
